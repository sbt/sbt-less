import com.typesafe.sbt.web.SbtWebPlugin
import com.typesafe.sbt.web.SbtWebPlugin._
import com.typesafe.sbt.less.SbtLessPlugin._

lazy val root = project.in(file(".")).addPlugins(SbtWebPlugin)

//JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

libraryDependencies += "org.webjars" % "bootstrap" % "3.0.2"

LessKeys.compress in WebKeys.Assets := false