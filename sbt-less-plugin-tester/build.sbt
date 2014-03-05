import com.typesafe.sbt.jse.SbtJsTaskPlugin

webSettings

SbtJsTaskPlugin.jsEngineAndTaskSettings

lessSettings

//JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

libraryDependencies += "org.webjars" % "bootstrap" % "3.0.2"

LessKeys.compress in WebKeys.Assets := false