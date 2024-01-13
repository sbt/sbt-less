lazy val root = (project in file(".")).enablePlugins(SbtWeb)

//JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

libraryDependencies += "org.webjars" % "bootstrap" % "3.0.2"

Assets / LessKeys.compress := false

Assets / LessKeys.less / includeFilter := "foo.less" | "bar.less"
