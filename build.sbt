organization := "com.typesafe.sbt"
name := "sbt-less"
description := "sbt-web less plugin"

scalaVersion := "2.10.4"
sbtPlugin := true

libraryDependencies ++= Seq(
  "org.webjars" % "less-node" % "2.5.0",
  "org.webjars" % "source-map" % "0.1.40-1",
  "org.webjars" % "mkdirp" % "0.5.0",
  "org.webjars" % "clean-css" % "2.2.7",
  "org.webjars" % "es6-promise-node" % "2.1.1"
)

addSbtPlugin("com.typesafe.sbt" %% "sbt-js-engine" % "1.1.2")

publishMavenStyle := false

publishTo := {
  if (isSnapshot.value) Some(Classpaths.sbtPluginSnapshots)
  else Some(Classpaths.sbtPluginReleases)
}

scriptedSettings

scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }

scriptedBufferLog := false
