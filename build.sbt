lazy val `sbt-less` = project in file(".")
description := "sbt-web less plugin"

libraryDependencies ++= Seq(
  "org.webjars" % "less-node" % "2.5.0",
  "org.webjars" % "source-map" % "0.1.40-1",
  "org.webjars" % "mkdirp" % "0.5.0",
  "org.webjars" % "clean-css" % "2.2.7",
  "org.webjars" % "es6-promise-node" % "2.1.1"
)

addSbtJsEngine("1.1.2")
