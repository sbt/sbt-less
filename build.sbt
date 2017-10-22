lazy val `sbt-less` = project in file(".")
description := "sbt-web less plugin"

libraryDependencies ++= Seq(
  "org.webjars" % "less-node" % "2.7.2",
  "org.webjars" % "mkdirp" % "0.5.0",
  "org.webjars.npm" % "clean-css" % "4.0.5",
  "org.webjars.npm" % "less-plugin-clean-css" % "1.5.1" intransitive(),
  "org.webjars" % "es6-promise-node" % "2.1.1"
)

addSbtJsEngine("1.2.2")
addSbtWeb("1.4.3")
