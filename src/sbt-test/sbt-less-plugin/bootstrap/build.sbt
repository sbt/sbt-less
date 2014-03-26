import com.typesafe.sbt.web.SbtWebPlugin

lazy val root = project.in(file(".")).addPlugins(SbtWebPlugin)

libraryDependencies += "org.webjars" % "bootstrap" % "3.0.2"
