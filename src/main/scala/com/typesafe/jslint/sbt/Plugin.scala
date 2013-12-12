package com.typesafe.jslint.sbt

import sbt.Keys._
import sbt._
import akka.actor.ActorRef
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import spray.json._
import xsbti.{Maybe, Position, Severity}
import java.lang.RuntimeException

/**
 * The WebDriver sbt plugin plumbing around the JslintEngine
 */
object Plugin extends sbt.Plugin {

  //import com.typesafe.sbt.web.WebPlugin.WebKeys._

  object LessKeys {
    val less = TaskKey[Unit]("less", "Invoke the less compiler.")
    val silent = SettingKey[Boolean]("less-silent", "Suppress output of error messages.")
    val verbose = SettingKey[Boolean]("less-verbose", "Be verbose.")
    val ieCompat = SettingKey[Boolean]("less-ie-compat", "Do IE compatibility checks.")
    val compress = SettingKey[Boolean]("less-compress", "Compress output by removing some whitespaces.")
    val cleancss = SettingKey[Boolean]("less-cleancss", "Compress output using clean-css.")
    val sourceMap = SettingKey[Boolean]("less-source-map", "Outputs a v3 sourcemap.")
    val sourceMapBasepath = SettingKey[String]("less-source-map-basepath", "Sets sourcemap base path, defaults to unmanaged resources.")
    val sourceMapRootpath = SettingKey[Option[String]]("less-source-map-rotpath", "Adds this path onto the sourcemap filename and less file paths.")
    val maxLineLen = SettingKey[Int]("less-max-line-len", "Maximum line length.")
    val strictMath = SettingKey[Boolean]("less-strict-math", "Requires brackets. This option may default to true and be removed in future.")
    val strictUnits = SettingKey[Boolean]("less-strict-units", "Whether all unit should be strict, or if mixed units are allowed.")
    val optimization = SettingKey[Int]("less-optimization", "Set the parser's optimization level.")
    val color = SettingKey[Boolean]("less-color", "Whether LESS output should be colorised")
    val insecure = SettingKey[Boolean]("less-insecure", "Allow imports from insecure https hosts.")
    val rootpath = SettingKey[Option[String]]("less-rootpath", "Set rootpath for url rewriting in relative imports and urls.")
    val relativeUrls = SettingKey[Boolean]("less-relative-urls", "Re-write relative urls to the base less file.")
  }

  import LessKeys._

  def lessSettings = Seq(
    silent := false,
    verbose := false,
    ieCompat := true,
    compress := false,
    cleancss := false,
    sourceMap := true,
//    sourceMapBasepath := resourceManaged.value,
    sourceMapRootpath := None,
    maxLineLen := -1,
    strictMath := false,
    strictUnits := false,
    optimization := 1,
    color := ConsoleLogger.formatEnabled,
    insecure := false,
    relativeUrls := false
  )


  /*
  private def lessTask(browser: ActorRef,
                         sources: Seq[File],
                         s: TaskStreams,
                         reporter: LoggerReporter
                          ): Unit = {

    reporter.reset()



    val pendingResults = lintForSources(jslintOptions, browser, sources)

    val results = Await.result(pendingResults, 10.seconds)
    results.foreach { result =>
      logErrors(reporter, s.log, result._1, result._2)
    }
    reporter.printSummary()
    if (reporter.hasErrors()) {
      throw new LintingFailedException
    }
  }

  private val jslinter = Jslinter()

  /*
   * lints a sequence of sources and returns a future representing the results of all.
   */
  private def lintForSources(options: JsObject, browser: ActorRef, sources: Seq[File]): Future[Seq[(File, JsArray)]] = {
    jslinter.beginLint(browser).flatMap[Seq[(File, JsArray)]] {
      lintState =>
        val results = sources.map {
          source =>
            val lintResult = jslinter.lint(lintState, source, options)
              .map(result => (source, result))
            lintResult.onComplete {
              case _ => jslinter.endLint(lintState)
            }
            lintResult
        }
        Future.sequence(results)
    }
  }

  private def logErrors(reporter: LoggerReporter, log: Logger, source: File, jslintErrors: JsArray): Unit = {

    jslintErrors.elements.map {
      case o: JsObject =>

        def getReason(o: JsObject): String = o.fields.get("reason").get.toString()

        def logWithSeverity(o: JsObject, s: Severity): Unit = {
          val p = new Position {
            def line(): Maybe[Integer] = Maybe.just(Integer.parseInt(o.fields.get("line").get.toString()))

            def lineContent(): String = o.fields.get("evidence") match {
              case Some(JsString(line)) => line
              case _ => ""
            }

            def offset(): Maybe[Integer] = Maybe.just(Integer.parseInt(o.fields.get("character").get.toString()) - 1)

            def pointer(): Maybe[Integer] = offset()

            def pointerSpace(): Maybe[String] = Maybe.just(
              lineContent().take(pointer().get).map {
                case '\t' => '\t'
                case x => ' '
              })

            def sourcePath(): Maybe[String] = Maybe.just(source.getPath)

            def sourceFile(): Maybe[File] = Maybe.just(source)
          }
          val r = getReason(o)
          reporter.log(p, r, s)
        }

        o.fields.get("id") match {
          case Some(JsString("(error)")) => logWithSeverity(o, Severity.Error)
          case Some(JsString("(info)")) => logWithSeverity(o, Severity.Info)
          case Some(JsString("(warn)")) => logWithSeverity(o, Severity.Warn)
          case Some(id@_) => log.error(s"Unknown type of error: $id with reason: ${getReason(o)}")
          case _ => log.error(s"Malformed error with reason: ${getReason(o)}")
        }
      case x@_ => log.error(s"Malformed result: $x")
    }
  }
  */
}