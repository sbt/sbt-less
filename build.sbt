sbtPlugin := true

organization := "com.typesafe.sbt"

name := "sbt-less"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.webjars" % "less-node" % "1.7.5",
  "org.webjars" % "source-map" % "0.1.31-2",
  "org.webjars" % "mkdirp" % "0.3.5",
  "org.webjars" % "clean-css" % "2.2.7"
)

addSbtPlugin("com.typesafe.sbt" %% "sbt-js-engine" % "1.1.1")

publishMavenStyle := false

publishTo := {
  if (isSnapshot.value) Some(Classpaths.sbtPluginSnapshots)
  else Some(Classpaths.sbtPluginReleases)
}

scriptedSettings

scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }

scriptedBufferLog := false
