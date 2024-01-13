lazy val root = (project in file(".")).enablePlugins(SbtWeb)

//JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

libraryDependencies += "org.webjars" % "bootstrap" % "3.4.1"

Assets / LessKeys.compress := false

Assets / LessKeys.less / includeFilter := "foo.less" | "bar.less"
