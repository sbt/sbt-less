package com.typesafe.sbt.less

import sbt._
import sbt.Keys._
import com.typesafe.sbt.web._
import com.typesafe.sbt.jse.SbtJsTask
import spray.json._

object Import {

  object LessKeys {
    val less = TaskKey[Seq[File]]("less", "Invoke the less compiler.")

    val cleancss = SettingKey[Boolean]("less-cleancss", "Compress output using clean-css.")
    val cleancssOptions = SettingKey[String]("less-cleancss-options", "Pass an option to clean css, using CLI arguments from https://github.com/GoalSmashers/clean-css .")
    val color = SettingKey[Boolean]("less-color", "Whether LESS output should be colorised")
    val compress = SettingKey[Boolean]("less-compress", "Compress output by removing some whitespaces.")
    val globalVariables = SettingKey[Seq[(String, String)]]("less-global-variables", "Variables that will be placed at the top of the less file.")
    val ieCompat = SettingKey[Boolean]("less-ie-compat", "Do IE compatibility checks.")
    val insecure = SettingKey[Boolean]("less-insecure", "Allow imports from insecure https hosts.")
    val maxLineLen = SettingKey[Int]("less-max-line-len", "Maximum line length.")
    val modifyVariables = SettingKey[Seq[(String, String)]]("less-modify-variables", "Modifies a variable already declared in the file.")
    val optimization = SettingKey[Int]("less-optimization", "Set the parser's optimization level.")
    val relativeImports = SettingKey[Boolean]("less-relative-imports", "Re-write import paths relative to the base less file.")
    val relativeUrls = SettingKey[Boolean]("less-relative-urls", "Re-write relative urls to the base less file.")
    val rootpath = SettingKey[String]("less-rootpath", "Set rootpath for url rewriting in relative imports and urls.")
    val silent = SettingKey[Boolean]("less-silent", "Suppress output of error messages.")
    val sourceMap = SettingKey[Boolean]("less-source-map", "Outputs a v3 sourcemap.")
    val sourceMapFileInline = SettingKey[Boolean]("less-source-map-file-inline", "Whether the source map should be embedded in the output file")
    val sourceMapLessInline = SettingKey[Boolean]("less-source-map-less-inline", "Whether to embed the less code in the source map")
    val sourceMapRootpath = SettingKey[String]("less-source-map-rootpath", "Adds this path onto the sourcemap filename and less file paths.")
    val strictImports = SettingKey[Boolean]("less-scrict-imports", "Whether imports should be strict.")
    val strictMath = SettingKey[Boolean]("less-strict-math", "Requires brackets. This option may default to true and be removed in future.")
    val strictUnits = SettingKey[Boolean]("less-strict-units", "Whether all unit should be strict, or if mixed units are allowed.")
    val urlArgs = SettingKey[String]("less-url-args", "Adds params into url tokens (e.g. 42, cb=42 or 'a=1&b=2').")
    val verbose = SettingKey[Boolean]("less-verbose", "Be verbose.")
  }

}

object SbtLess extends AutoPlugin {

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import autoImport.LessKeys._

  val lessUnscopedSettings = Seq(

    includeFilter := GlobFilter("main.less"),

    jsOptions := JsObject(
      "cleancss" -> JsBoolean(cleancss.value),
      "cleancssOptions" -> JsString(cleancssOptions.value),
      "color" -> JsBoolean(color.value),
      "compress" -> JsBoolean(compress.value),
      "globalVars" -> toJsObjectOrNull(globalVariables.value),
      "modifyVars" -> toJsObjectOrNull(modifyVariables.value),
      "ieCompat" -> JsBoolean(ieCompat.value),
      "insecure" -> JsBoolean(insecure.value),
      "maxLineLen" -> JsNumber(maxLineLen.value),
      "optimization" -> JsNumber(optimization.value),
      "paths" -> JsArray(
        (sourceDirectories.value
            ++ resourceDirectories.value
            ++ webModuleDirectories.value
            ++ nodeModuleDirectories.value)
          .map(f => JsString(f.getAbsolutePath)).toVector
      ),
      "relativeImports" -> JsBoolean(relativeImports.value),
      "relativeUrls" -> JsBoolean(relativeUrls.value),
      "rootpath" -> JsString(rootpath.value),
      "silent" -> JsBoolean(silent.value),
      "sourceMap" -> JsBoolean(sourceMap.value),
      "sourceMapFileInline" -> JsBoolean(sourceMapFileInline.value),
      "sourceMapLessInline" -> JsBoolean(sourceMapLessInline.value),
      "sourceMapRootpath" -> JsString(sourceMapRootpath.value),
      "strictImports" -> JsBoolean(strictImports.value),
      "strictMath" -> JsBoolean(strictMath.value),
      "strictUnits" -> JsBoolean(strictUnits.value),
      "verbose" -> JsBoolean(verbose.value)
    ).toString()
  )

  private def toJsObjectOrNull(fields: Seq[(String, String)]): JsValue = {
    if (fields.isEmpty) JsNull
    else JsObject(fields.toMap.mapValues(v => JsString(v)))
  }

  override def buildSettings = inTask(less)(
    SbtJsTask.jsTaskSpecificUnscopedBuildSettings ++ Seq(
      moduleName := "less",
      shellFile := getClass.getClassLoader.getResource("lessc.js")
    )
  )

  override def projectSettings = Seq(
    cleancss := false,
    cleancssOptions := "",
    color := false,
    compress := false,
    globalVariables := Seq.empty,
    ieCompat := true,
    insecure := false,
    maxLineLen := -1,
    modifyVariables := Seq.empty,
    optimization := 1,
    relativeImports := true,
    relativeUrls := false,
    rootpath := "",
    silent := false,
    sourceMap := true,
    sourceMapFileInline := false,
    sourceMapLessInline := false,
    sourceMapRootpath := "",
    strictImports := false,
    strictMath := false,
    strictUnits := false,
    verbose := false

  ) ++ inTask(less)(
    SbtJsTask.jsTaskSpecificUnscopedProjectSettings ++
      inConfig(Assets)(lessUnscopedSettings) ++
      inConfig(TestAssets)(lessUnscopedSettings) ++
      Seq(
        taskMessage in Assets := "LESS compiling",
        taskMessage in TestAssets := "LESS test compiling"
      )
  ) ++ SbtJsTask.addJsSourceFileTasks(less) ++ Seq(
    less in Assets := (less in Assets).dependsOn(webModules in Assets).value,
    less in TestAssets := (less in TestAssets).dependsOn(webModules in TestAssets).value
  )

}