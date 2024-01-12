lazy val `sbt-less` = (project in file(".")).enablePlugins(SbtWebBase)
description := "sbt-web less plugin"

libraryDependencies ++= Seq(
  "org.webjars" % "less-node" % "2.7.2-1",
  "org.webjars" % "mkdirp" % "0.5.0",
  "org.webjars.npm" % "clean-css" % "4.0.5",
  "org.webjars.npm" % "less-plugin-clean-css" % "1.5.1" intransitive(),
  "org.webjars" % "es6-promise-node" % "2.1.1"
)

addSbtJsEngine("1.3.5")
addSbtWeb("1.5.3")
