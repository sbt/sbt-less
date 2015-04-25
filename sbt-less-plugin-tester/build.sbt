lazy val root = (project in file(".")).enablePlugins(SbtWeb)

// JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

libraryDependencies += "org.webjars" % "bootstrap" % "3.0.2"

LessKeys.compress in Assets := false

includeFilter in (Assets, LessKeys.less) := "foo.less" | "bar.less"
