package com.typesafe.less.sbt

import sbt.Keys._
import sbt._
import sbt.KeyRanks._
import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import com.typesafe.jse.{Rhino, PhantomJs, Node, CommonNode, Trireme}

import com.typesafe.jse.sbt.JsEnginePlugin.JsEngineKeys
import com.typesafe.web.sbt.WebPlugin.WebKeys
import xsbti.{Severity, Problem}
import com.typesafe.less.{LessCompiler, LessError, LessResult}
import com.typesafe.web.sbt.{WebPlugin, GeneralProblem, LineBasedProblem}
import akka.util.Timeout
import scala.collection.immutable
import com.typesafe.web.sbt.CompileProblems
import com.typesafe.web.sbt.incremental
import com.typesafe.web.sbt.incremental._
import com.typesafe.less.LessCompileError
import com.typesafe.less.LessOptions
import com.typesafe.less.LessSuccess

object LessPlugin extends sbt.Plugin {

  object LessKeys {
    // Less command
    val less = TaskKey[Seq[File]]("less", "Invoke the less compiler.")

    val lessFilter = SettingKey[FileFilter]("less-filter", "The filter for less files.")
    val lessSources = TaskKey[PathFinder]("less-sources", "The list of less sources.", ASetting)

    // Internal
    val lesscSource = TaskKey[File]("lessc-source", "The extracted lessc source file.", CSetting)
    val lessOptions = TaskKey[LessOptions]("less-options", "The less options", CSetting)
    val partialLessOptions = TaskKey[(Int, Boolean, Boolean, Boolean, Int, Boolean, Boolean, Boolean) => LessOptions]("less-options-partial")

    // Less options
    val silent = SettingKey[Boolean]("less-silent", "Suppress output of error messages.", ASetting)
    val verbose = SettingKey[Boolean]("less-verbose", "Be verbose.", ASetting)
    val ieCompat = SettingKey[Boolean]("less-ie-compat", "Do IE compatibility checks.", ASetting)
    val compress = SettingKey[Boolean]("less-compress", "Compress output by removing some whitespaces.", ASetting)
    val cleancss = SettingKey[Boolean]("less-cleancss", "Compress output using clean-css.", ASetting)
    val includePaths = SettingKey[Seq[File]]("less-include-paths", "The include paths to search when looking for LESS imports", ASetting)
    val includePathGenerators = SettingKey[Seq[Task[Seq[File]]]]("less-include-path-generators", "The generators for include paths", ASetting)
    val allIncludePaths = TaskKey[Seq[File]]("less-all-include-paths", "All the include paths", DTask)
    val sourceMap = SettingKey[Boolean]("less-source-map", "Outputs a v3 sourcemap.", ASetting)
    val sourceMapLessInline = SettingKey[Boolean]("less-source-map-less-inline", "Whether to embed the less code in the source map", ASetting)
    val sourceMapFileInline = SettingKey[Boolean]("less-source-map-file-inline", "Whether the source map should be embedded in the output file", ASetting)
    val sourceMapRootpath = SettingKey[Option[String]]("less-source-map-rootpath", "Adds this path onto the sourcemap filename and less file paths.", ASetting)
    val maxLineLen = SettingKey[Int]("less-max-line-len", "Maximum line length.", ASetting)
    val strictMath = SettingKey[Boolean]("less-strict-math", "Requires brackets. This option may default to true and be removed in future.", ASetting)
    val strictUnits = SettingKey[Boolean]("less-strict-units", "Whether all unit should be strict, or if mixed units are allowed.", ASetting)
    val strictImports = SettingKey[Boolean]("less-scrict-imports", "Whether imports should be strict.", ASetting)
    val optimization = SettingKey[Int]("less-optimization", "Set the parser's optimization level.", ASetting)
    val color = SettingKey[Boolean]("less-color", "Whether LESS output should be colorised", ASetting)
    val insecure = SettingKey[Boolean]("less-insecure", "Allow imports from insecure https hosts.", ASetting)
    val rootpath = SettingKey[Option[String]]("less-rootpath", "Set rootpath for url rewriting in relative imports and urls.", ASetting)
    val relativeUrls = SettingKey[Boolean]("less-relative-urls", "Re-write relative urls to the base less file.", ASetting)
  }

  import LessKeys._
  import JsEngineKeys._
  import WebKeys._

  private val defaults = LessOptions()

  private val unscopedSettings = Seq(
    silent := defaults.silent,
    verbose := defaults.verbose,
    ieCompat := defaults.ieCompat,
    compress := defaults.compress,
    cleancss := defaults.cleancss,
    includePaths := defaults.includePaths,
    sourceMap := defaults.sourceMap,
    sourceMapLessInline := defaults.sourceMapLessInline,
    sourceMapFileInline := defaults.sourceMapFileInline,
    sourceMapRootpath := defaults.sourceMapRootpath,
    maxLineLen := defaults.maxLineLen,
    strictMath := defaults.strictMath,
    strictUnits := defaults.strictUnits,
    strictImports := defaults.strictImports,
    optimization := defaults.optimization,
    color := ConsoleLogger.formatEnabled,
    insecure := defaults.insecure,
    rootpath := defaults.rootpath,
    relativeUrls := defaults.relativeUrls,

    includePathGenerators := Nil,
    includePathGenerators <+= includePaths.map(identity),
    includePathGenerators <+= webJars.map(Seq(_)),
    allIncludePaths <<= Defaults.generate(includePathGenerators),

    // Using a partial here given that we need to work around param # limitations.
    partialLessOptions <<= (silent, verbose, ieCompat, compress, cleancss, allIncludePaths, sourceMap, sourceMapLessInline,
      sourceMapFileInline, sourceMapRootpath, rootpath).map {
      (s, v, ie, co, cl, ip, sm, sl, sf, sr, r) =>
        LessOptions(s, v, ie, co, cl, ip, sm, sl, sf, sr, r, _, _, _, _, _, _, _, _)
    },

    lessOptions <<= (partialLessOptions, maxLineLen, strictMath, strictUnits, strictImports, optimization, color,
      insecure, relativeUrls).map {
      (partialLessOptions, mll, sm, su, si, o, c, i, rl) =>
        partialLessOptions(mll, sm, su, si, o, c, i, rl)
    },

    lessSources := (unmanagedSources.value ** lessFilter.value).get

  )

  def lessSettings = Seq(
    lesscSource in LocalRootProject <<= (target in LocalRootProject).map {
      target =>
        WebPlugin.copyResourceTo(
          target / "less-plugin",
          "lessc.js",
          LessPlugin.getClass.getClassLoader
        )
    },

    lessFilter in Assets := GlobFilter("*.less"),
    lessFilter in TestAssets := (lessFilter in Assets).value,

    less in Assets <<= lessTask(Assets),
    less in TestAssets <<= lessTask(TestAssets),
    less <<= less in Assets,

    compile in Compile <<= (compile in Compile).dependsOn(less in Assets),
    test in Test <<= (test in Test).dependsOn(less in Assets, less in TestAssets)

  ) ++ inConfig(Assets)(unscopedSettings) ++ inConfig(TestAssets)(unscopedSettings)

  private def lessTask(scope: Configuration) = (state, lesscSource in LocalRootProject, nodeModules in Plugin,
    unmanagedSourceDirectories in scope, lessSources in scope, resourceManaged in scope,
    lessOptions in scope, engineType, streams, reporter, parallelism).map(lessCompiler)

  /**
   * The less compiler task
   */
  def lessCompiler(state: State,
                   lessc: File,
                   nodeModules: File,
                   sourceFolders: Seq[File],
                   lessSources: PathFinder,
                   outputDir: File,
                   lessOptions: LessOptions,
                   engineType: EngineType.Value,
                   s: TaskStreams,
                   reporter: LoggerReporter,
                   parallelism: Int
                    ): Seq[File] = {

    import com.typesafe.web.sbt.WebPlugin._

    val timeoutPerSource = 10.seconds

    val engineProps = engineType match {
      case EngineType.CommonNode => CommonNode.props()
      case EngineType.Node => Node.props(stdModulePaths = immutable.Seq(nodeModules.getCanonicalPath))
      case EngineType.PhantomJs => PhantomJs.props()
      case EngineType.Rhino => Rhino.props()
      case EngineType.Trireme => Trireme.props(stdModulePaths = immutable.Seq(nodeModules.getCanonicalPath))

    }

    outputDir.mkdirs()

    val files = (lessSources.get x relativeTo(sourceFolders)).map {
      case (file, relative) =>
        // Drop the .less, and add either .css or .min.css
        val extension = if (lessOptions.compress) ".min.css" else ".css"
        val outName = relative.replaceAll("\\.less$", "") + extension

        val out = outputDir / outName
        file -> out
    }

    implicit val opInputHasher =
      OpInputHasher[(File, File)](sourceMapping => OpInputHash.hashString(sourceMapping + "|" + lessOptions))

    val problems: Seq[Problem] = incremental.runIncremental(s.cacheDirectory, files) {
      modifiedFiles: Seq[(File, File)] =>

        if (modifiedFiles.size > 0) {
          s.log.info(s"Compiling ${modifiedFiles.size} less files")

          val fileBatches = (modifiedFiles grouped Math.max(modifiedFiles.size / parallelism, 1)).toSeq
          val pendingResultBatches: Seq[Future[Seq[LessResult]]] = fileBatches.map {
            sourceBatch =>
              implicit val timeout = Timeout(timeoutPerSource * sourceBatch.size)
              withActorRefFactory(state, this.getClass.getName) {
                arf =>
                  val engine = arf.actorOf(engineProps)
                  val compiler = new LessCompiler(engine, lessc)
                  compiler.compile(sourceBatch, lessOptions).map {
                    result =>
                      if (!result.stdout.isEmpty) s.log.info(result.stdout)
                      if (!result.stderr.isEmpty) s.log.error(result.stderr)
                      result.results
                  }
              }
          }

          val allLessResults = Await.result(Future.sequence(pendingResultBatches), timeoutPerSource * modifiedFiles.size).flatten

          val results: Map[(File, File), OpResult] = allLessResults.map {
            entry =>
              val result = entry match {
                case LessSuccess(input, output, dependsOn) =>
                  OpSuccess(dependsOn + input, Set(output))
                case _ => OpFailure
              }
              (entry.input -> entry.output) -> result
          }.toMap

          def lineContent(file: File, line: Int) = IO.readLines(file).drop(line - 2).headOption

          val problems = allLessResults.map {
            case e: LessError =>
              e.compileErrors.map {
                case LessCompileError(Some(input), Some(line), Some(column), message) =>
                  new LineBasedProblem(message, Severity.Error, line, column - 1, lineContent(input, line).getOrElse(""), input)

                case LessCompileError(Some(input), _, _, message) =>
                  new GeneralProblem(message, input)
              }
            case _ => Nil
          }.flatten.distinct

          (results, problems)

        } else {
          (Map.empty, Seq.empty)
        }
    }

    CompileProblems.report(reporter, problems)

    files.map(_._2)
  }

}
