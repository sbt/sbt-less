package com.typesafe.less

import akka.actor.ActorRef
import akka.pattern.ask
import java.io.File
import spray.json._
import akka.util.Timeout
import scala.concurrent.{ExecutionContext, Future}
import com.typesafe.jse.Engine
import com.typesafe.jse.Engine.JsExecutionResult
import scala.util.control.NonFatal

/**
 * The less options.
 *
 * @param silent Suppress output of error messages.
 * @param verbose Be verbose.
 * @param ieCompat Do IE compatibility checks.
 * @param compress Compress output by removing some whitespaces.
 * @param cleancss Compress output using clean-css.
 * @param includePaths The include paths to search when looking for LESS imports.
 * @param sourceMap Outputs a v3 sourcemap.
 * @param sourceMapLessInline Whether to embed the less code in the source map.
 * @param sourceMapFileInline Whether the source map should be embedded in the output file.
 * @param sourceMapRootpath Adds this path onto the sourcemap filename and less file paths.
 * @param maxLineLen Maximum line length.
 * @param strictMath Requires brackets. This option may default to true and be removed in future.
 * @param strictUnits Whether all unit should be strict, or if mixed units are allowed.
 * @param strictImports Whether imports should be strict.
 * @param optimization Set the parser's optimization level.
 * @param color Whether LESS output should be colorised.
 * @param insecure Allow imports from insecure https hosts.
 * @param rootpath Set rootpath for url rewriting in relative imports and urls.
 * @param relativeUrls Re-write relative urls to the base less file.
 */
case class LessOptions(
                        silent: Boolean = false,
                        verbose: Boolean = false,
                        ieCompat: Boolean = true,
                        compress: Boolean = false,
                        cleancss: Boolean = false,
                        includePaths: Seq[File] = Seq(),
                        sourceMap: Boolean = true,
                        sourceMapLessInline: Boolean = false,
                        sourceMapFileInline: Boolean = false,
                        sourceMapRootpath: Option[String] = None,
                        rootpath: Option[String] = None,
                        maxLineLen: Int = -1,
                        strictMath: Boolean = false,
                        strictUnits: Boolean = false,
                        strictImports: Boolean = false,
                        optimization: Int = 1,
                        color: Boolean = true,
                        insecure: Boolean = false,
                        relativeUrls: Boolean = false
                        )

/** The result of compiling a less file. */
sealed trait LessResult {

  def input: File

  def output: File
}

/**
 * A successful less compilation.
 */
case class LessSuccess(input: File, output: File, dependsOn: Set[File]) extends LessResult

/**
 * A compile error.
 */
case class LessCompileError(filename: Option[File], line: Option[Int], column: Option[Int], message: String)

/**
 * An erroneous less compilation.
 */
case class LessError(input: File, output: File, compileErrors: Seq[LessCompileError]) extends LessResult

/**
 * The result of compiling many less files.
 */
case class LessExecutionResult(results: Seq[LessResult], stdout: String, stderr: String)

/**
 * JSON protocol for the LESS result deserialisation.
 */
object LessResultProtocol extends DefaultJsonProtocol {

  implicit object FileFormat extends RootJsonFormat[File] {
    def write(f: File) = JsString(f.getCanonicalPath)

    def read(value: JsValue) = value match {
      case s: JsString => new File(s.convertTo[String])
      case _ => deserializationError("String expected")
    }
  }

  implicit val lessSuccess: RootJsonFormat[LessSuccess] = jsonFormat3(LessSuccess.apply)
  implicit val lessCompileError = jsonFormat4(LessCompileError.apply)
  implicit val lessError = jsonFormat3(LessError.apply)

  implicit val lessResult = new JsonFormat[LessResult] {
    def read(json: JsValue) = json.asJsObject.fields.get("status") match {
      case Some(JsString("success")) => json.convertTo[LessSuccess]
      case Some(JsString("failure")) => json.convertTo[LessError]
      case _ => throw new IllegalArgumentException("Unable to extract less result from JSON")
    }

    def writeStatus[C](status: String, c: C)(implicit format: JsonFormat[C]) = {
      JsObject(format.write(c).asJsObject.fields + ("status" -> JsString(status)))
    }

    def write(result: LessResult) = result match {
      case success: LessSuccess => writeStatus("success", success)
      case error: LessError => writeStatus("failure", error)
    }
  }

}

/**
 * A less compiler
 *
 * @param engine The Javascript engine
 * @param shellSource The path of the lessc source file
 */
class LessCompiler(engine: ActorRef, shellSource: File) {

  /**
   * Compile the given files using the given options.
   *
   * @param filesToCompile A tuple of input/output elements for files to compile
   * @param opts The less options
   * @param timeout The timeout
   * @return The result of the execution
   */
  def compile(filesToCompile: Seq[(File, File)], opts: LessOptions)(implicit timeout: Timeout): Future[LessExecutionResult] = {

    val bool = JsBoolean.apply _

    val jsOptions: Map[String, JsValue] = Map(
      "silent" -> bool(opts.silent),
      "verbose" -> bool(opts.verbose),
      "ieCompat" -> bool(opts.ieCompat),
      "compress" -> bool(opts.compress),
      "cleancss" -> bool(opts.cleancss),
      "sourceMap" -> bool(opts.sourceMap),
      "sourceMapFileInline" -> bool(opts.sourceMapFileInline),
      "sourceMapLessInline" -> bool(opts.sourceMapLessInline),
      "max_line_len" -> JsNumber(opts.maxLineLen),
      "strictMath" -> bool(opts.strictMath),
      "strictUnits" -> bool(opts.strictUnits),
      "strictImports" -> bool(opts.strictImports),
      "optimization" -> JsNumber(opts.optimization),
      "color" -> bool(opts.color),
      "insecure" -> bool(opts.insecure),
      "relativeurls" -> bool(opts.relativeUrls),
      "globalVariables" -> JsString(""),
      "modifyVariables" -> JsString(""),
      "paths" -> JsArray(opts.includePaths.map(p => JsString(p.getAbsolutePath)).toList),
      "rootpath" -> JsString(opts.rootpath.getOrElse("")),
      "sourceMapRootpath" -> JsString(opts.sourceMapRootpath.getOrElse(""))
    )

    import ExecutionContext.Implicits.global

    val options = JsArray(filesToCompile.toList.map {
      case (in, out) =>
        val sourceMapArgs = if (opts.sourceMap && !opts.sourceMapFileInline) {
          Map(
            "sourceMapFilename" -> JsString(out.getAbsolutePath + ".map"),
            "sourceMapBasepath" -> JsString(in.getParent)
          )
        } else Map.empty
        val inputOutputArgs = Map(
          "input" -> JsString(in.getAbsolutePath),
          "output" -> JsString(out.getAbsolutePath)
        )
        JsObject(jsOptions ++ sourceMapArgs ++ inputOutputArgs)
    }).toString()
    val args = List(options)
    (engine ? Engine.ExecuteJs(shellSource, args, timeout = timeout.duration)).map {
      case JsExecutionResult(exitValue, output, error) =>

        val stdout = new String(output.toArray, "UTF-8")
        val stderr = new String(error.toArray, "UTF-8")

        // Last line of the results should be the status
        val outputLines = stdout.split("\n")
        val status = outputLines.lastOption.getOrElse("{}")

        // Try and parse
        try {
          import LessResultProtocol._
          val results = JsonParser(status).convertTo[Seq[LessResult]]
          // Don't include the status line (ie the last line) in stdout, we exclude it by doing dropRight(1).
          def trimmed(out: Array[String]): String = if (out.isEmpty) "" else out.dropRight(1).mkString("\n")
          LessExecutionResult(results, trimmed(outputLines), stderr)
        } catch {
          case NonFatal(e) =>
            val results = filesToCompile.map {
              case (in, out) => LessError(
                in,
                out,
                Seq(LessCompileError(None, None, None, s"Fatal error in less compiler: ${e.getMessage}"))
              )
            }
            LessExecutionResult(results, new String(output.toArray), stderr)
        }
    }
  }
}
