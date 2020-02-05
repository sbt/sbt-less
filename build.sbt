lazy val `sbt-less` = project in file(".")
enablePlugins(SbtWebBase)

description := "sbt-web less plugin"

libraryDependencies ++= Seq(
  "org.webjars" % "less-node" % "3.8.1",
  "org.webjars.npm" % "clone" % "2.0.0",
  "org.webjars" % "mkdirp" % "0.5.0",
  "org.webjars.npm" % "clean-css" % "4.2.1",
  "org.webjars.npm" % "es6-promise" % "4.2.8"
)

addSbtJsEngine("1.2.3")
addSbtWeb("1.4.4")
