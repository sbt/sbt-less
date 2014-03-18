import com.typesafe.sbt.web.SbtWebPlugin._
import com.typesafe.sbt.less.SbtLessPlugin._

//JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

libraryDependencies += "org.webjars" % "bootstrap" % "3.0.2"

LessKeys.compress in WebKeys.Assets := false