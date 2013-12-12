package com.typesafe.less

import akka.actor.ActorRef
import akka.pattern.ask
import java.io.File
import scala.collection.immutable
import spray.json._
import akka.util.Timeout
import scala.concurrent.{ExecutionContext, Future}
import com.typesafe.jse.Engine
import com.typesafe.jse.Engine.JsExecutionResult
import org.webjars.WebJarExtractor
import scala.util.control.NonFatal

case class LessOptions(
  silent: Boolean = false,
  verbose: Boolean = false,
  ieCompat: Boolean = true,
  compress: Boolean = false,
  cleancss: Boolean = false,
  includePaths: Seq[File] = Seq(),
  sourceMap: Boolean = false,
  sourceMapLessInline: Boolean = false,
  sourceMapFileInline: Boolean = false,
  sourceMapRootpath: String = "",
  rootpath: String = "",
  maxLineLen: Int = -1,
  strictMath: Boolean = false,
  strictUnits: Boolean = false,
  strictImports: Boolean = false,
  optimization: Int = 1,
  color: Boolean = true,
  insecure: Boolean = false,
  relativeUrls: Boolean = false
)

sealed trait LessResult {
  def inputFile: String
}
case class LessSuccess(inputFile: String, dependsOn: Set[String]) extends LessResult
case class LessCompileError(filename: Option[String], line: Option[Int], column: Option[Int], message: String)
case class LessError(inputFile: String, compileErrors: Seq[LessCompileError]) extends LessResult

case class LessExecutionResult(results: Seq[LessResult], stdout: String, stderr: String)

object LessResult extends DefaultJsonProtocol {

  implicit val lessSuccess: RootJsonFormat[LessSuccess] = jsonFormat2(LessSuccess.apply)
  implicit val lessCompileError = jsonFormat4(LessCompileError.apply)
  implicit val lessError = jsonFormat2(LessError.apply)

  implicit val lessResult = new JsonFormat[LessResult] {
    def read(json: JsValue) = json.asJsObject.fields.get("status") match {
      case Some(JsString("success")) => json.convertTo[LessSuccess]
      case Some(JsString("failure")) => json.convertTo[LessError]
    }

    def writeStatus[C](status: String, c: C)(implicit format : JsonFormat[C]) = {
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
 * @param modulePaths The module paths to add.  This must at a minimum contain less on it, but should also contain
 *                    source-map and amdefine if source-map support is desired.
 */
class LessCompiler(engine: ActorRef, shellSource: File, modulePaths: immutable.Seq[String]) {

  /**
   * Compile the given files using the given options.
   *
   * @param filesToCompile A tuple of input/output elements for files to compile
   * @param opts The less options
   * @param timeout The timeout
   * @return The result of the execution
   */
  def compile(filesToCompile: immutable.Seq[(File, File)], opts: LessOptions)(implicit timeout: Timeout): Future[LessExecutionResult] = {

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
      "rootpath" -> JsString(opts.rootpath),
      "sourceMapRootpath" -> JsString(opts.sourceMapRootpath)
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
    (engine ? Engine.ExecuteJs(shellSource, args, modulePaths = modulePaths)).map {
      case JsExecutionResult(exitValue, output, error) => {

        val outputLines = new String(output.toArray).split("\n")

        // Last line of the results should be the status
        val status = outputLines.lastOption.getOrElse("{}")

        val stderr = new String(error.toArray)

        // Try and parse
        try {
          import DefaultJsonProtocol._
          val results = JsonParser(status).convertTo[Seq[LessResult]]
          val stdout = if (outputLines.isEmpty) "" else outputLines.dropRight(1).mkString("\n")

          LessExecutionResult(results, stdout, stderr)
        } catch {
          case NonFatal(_) => {
            val results = filesToCompile.map {
              case (in, _) => LessError(in.getAbsolutePath, Seq(LessCompileError(None, None, None, "Fatal error in less compiler")))
            }
            LessExecutionResult(results, new String(output.toArray), stderr)
          }
        }


      }
    }
  }
}
