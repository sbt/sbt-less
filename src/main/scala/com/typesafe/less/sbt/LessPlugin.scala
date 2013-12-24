package com.typesafe.less.sbt

import sbt.Keys._
import sbt._
import sbt.KeyRanks._
import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import org.webjars.{FileSystemCache, WebJarExtractor}
import com.typesafe.jse.{Rhino, PhantomJs, Node, CommonNode}
import com.typesafe.jse.sbt.JsEnginePlugin.JsEngineKeys
import com.typesafe.web.sbt.WebPlugin.WebKeys
import xsbti.{CompileFailed, Severity, Problem}
import com.typesafe.less.{LessResult, LessCompiler, LessError, LessOptions, LessSuccess, LessCompileError}
import com.typesafe.web.sbt.{GeneralProblem, LineBasedProblem}

/**
 * The WebDriver sbt plugin plumbing around the JslintEngine
 */
object LessPlugin extends sbt.Plugin {

  object LessKeys {
    // Less command
    val less = TaskKey[Seq[File]]("less", "Invoke the less compiler.")

    val lessSources = SettingKey[PathFinder]("less-sources", "The list of less sources.", ASetting)

    // Internal
    val lesscSource = TaskKey[File]("lessc-source", "The extracted lessc source file.", CSetting)
    val lessDir = SettingKey[File]("less-dir", "The working directory for less.", CSetting)
    val lesscDepsDir = SettingKey[File]("lessc-deps-dir", "The extracted lessc dependencies directory.")
    val lesscDeps = TaskKey[File]("lessc-deps", "Extract the lessc dependencies.", CSetting)
    val lesscDepsCacheFile = SettingKey[File]("lessc-deps-cache", "The cache file used to store the WebJarExtractor", CSetting)
    val lessOptions = SettingKey[LessOptions]("less-options", "The less options", CSetting)
    val lessOptionsIntermediate = SettingKey[(Int, Boolean, Boolean, Boolean, Int, Boolean, Boolean, Boolean) => LessOptions]("less-options-intermediate")

    // Less options
    val silent = SettingKey[Boolean]("less-silent", "Suppress output of error messages.", ASetting)
    val verbose = SettingKey[Boolean]("less-verbose", "Be verbose.", ASetting)
    val ieCompat = SettingKey[Boolean]("less-ie-compat", "Do IE compatibility checks.", ASetting)
    val compress = SettingKey[Boolean]("less-compress", "Compress output by removing some whitespaces.", ASetting)
    val cleancss = SettingKey[Boolean]("less-cleancss", "Compress output using clean-css.", ASetting)
    val includePaths = SettingKey[Seq[File]]("less-include-paths", "The include paths to search when looking for LESS imports", ASetting)
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
  def lessSettings = Seq(
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

    lessOptionsIntermediate <<= (silent, verbose, ieCompat, compress, cleancss, includePaths, sourceMap, sourceMapLessInline,
        sourceMapFileInline, sourceMapRootpath, rootpath).apply((s, v, ie, co, cl, ip, sm, sl, sf, sr, r) =>
      LessOptions(s, v, ie, co, cl, ip, sm, sl, sf, sr, r, _, _, _, _, _, _, _, _)),

    lessOptions <<= (lessOptionsIntermediate, maxLineLen, strictMath, strictUnits, strictImports, optimization, color,
      insecure, relativeUrls).apply((loi, mll, sm, su, si, o, c, i, rl) => loi(mll, sm, su, si, o, c, i, rl)),

    lessDir in LocalRootProject <<= (target in LocalRootProject).apply(_ / "lessc"),
    lesscDepsDir in LocalRootProject <<= (lessDir in LocalRootProject).apply(_ / "deps"),
    lesscDepsCacheFile in LocalRootProject <<= (lessDir in LocalRootProject).apply(_ / "deps.cache"),
    lesscDeps in LocalRootProject <<= (lesscDepsCacheFile, lesscDepsDir).map { (cacheFile, dir) =>
      cacheFile.getParentFile.mkdirs()
      val cache = new FileSystemCache(cacheFile)
      val extractor = new WebJarExtractor(cache, LessPlugin.getClass.getClassLoader)
      extractor.extractWebJarTo("less", dir)
      extractor.extractWebJarTo("source-map", dir)
      extractor.extractWebJarTo("amdefine", dir)
      cache.save()
      dir / "lib"
    },
    lesscSource in LocalRootProject <<= (lessDir in LocalRootProject).map { dir =>
      val is = this.getClass.getClassLoader.getResourceAsStream("lessc.js")
      try {
        val f = dir / "lessc.js"
        IO.transfer(is, f)
        f
      } finally {
        is.close()
      }
    },

    (lessSources in Assets) <<= (sourceDirectory in Assets)(base => (base ** "*.less") --- base ** "_*"),
    (lessSources in TestAssets) <<= (sourceDirectory in TestAssets)(base => (base ** "*.less") --- base ** "_*"),

    less <<= lessTask(Assets),

    resourceGenerators in Compile <+= less,
    resourceGenerators in Test <+= lessTask(TestAssets)
  )

  private def lessTask(scope: Configuration) = (state, lesscSource in LocalRootProject, lesscDeps in LocalRootProject,
    unmanagedSourceDirectories in scope, lessSources in scope, resourceManaged in scope,
    lessOptions, engineType, streams, reporter, parallelism).map(lessCompiler)

  def lessCompiler(state: State,
               lessc: File,
               lessDeps: File,
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

    // FIXME: Should be made configurable.
    implicit val duration = 1.hour

    val engineProps = engineType match {
      case EngineType.CommonNode => CommonNode.props()
      case EngineType.Node => Node.props()
      case EngineType.PhantomJs => PhantomJs.props()
      case EngineType.Rhino => Rhino.props()
    }

    val files = (lessSources.get x relativeTo(sourceFolders)).map {
      case (file, relative) =>
        // Drop the .less, and add either .css or .min.css
        val extension = if (lessOptions.compress) ".min.css" else ".css"
        val outName = relative.replaceAll("\\.less$", "") + extension

        val out = outputDir / outName
        file -> out
    }

    // Filter files from cache here

    s.log.info(s"Compiling ${files.size} less files")

    val fileBatches = (files grouped Math.max(files.size / parallelism, 1)).toSeq
    val pendingResultBatches: Seq[Future[Seq[(File, LessCompileError)]]] = fileBatches.map { sourceBatch =>
      withActorRefFactory(state, this.getClass.getName) { arf =>
        val engine = arf.actorOf(engineProps)
        val compiler = new LessCompiler(engine, lessc, Seq(lessDeps.getCanonicalPath))
        compiler.compile(files, lessOptions).map { result =>
          if (!result.stdout.isEmpty) s.log.info(result.stdout)
          if (!result.stderr.isEmpty) s.log.error(result.stderr)

          result.results.foldLeft[Seq[(File, LessCompileError)]](Nil) {
            case (problems, LessSuccess(file, deps)) =>
              // Handle caching here
              problems
            case (problems, LessError(file, errors)) =>
              val f = new File(file)
              problems ++ errors.map(f -> _)
          }
        }
      }
    }

    val compileErrors = Await.result(Future.sequence(pendingResultBatches), duration).flatten

    def lineContent(file: File, line: Int) = {
      IO.readLines(file).drop(line - 2).headOption
    }

    // Use distinct problems, because multiple entry points could have encountered the same compile error for the
    // same file
    val problems = compileErrors.distinct.map {
      case (_, LessCompileError(Some(f), Some(line), Some(column), message)) =>
        val file = new File(f)
        new LineBasedProblem(message, Severity.Error, line, column - 1, lineContent(file, line).getOrElse(""), file)

      case (file, LessCompileError(_, _, _, message)) => new GeneralProblem(message, file)
    }

    reporter.reset()
    problems.filter(_.severity() == Severity.Error).foreach(p => reporter.log(p.position, p.message, p.severity))
    reporter.printSummary()

    if (problems.exists(_.severity() == Severity.Error)) {
      throw new LessCompilationFailedException(problems.toArray)
    }

    files.map(_._2)
  }

  class LessCompilationFailedException(override val problems: Array[Problem])
    extends CompileFailed
    with FeedbackProvidedException {

    override val arguments: Array[String] = Array.empty
  }
}