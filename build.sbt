sbtPlugin := true

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

resolvers ++= Seq(
  Resolver.mavenLocal,
  "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.url("sbt snapshot plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
)

addSbtPlugin("com.typesafe.sbt" %% "sbt-js-engine" % "1.1.0")

publishMavenStyle := false

publishTo := {
  if (isSnapshot.value) Some(Classpaths.sbtPluginSnapshots)
  else Some(Classpaths.sbtPluginReleases)
}

scriptedSettings

scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }

scriptedBufferLog := false
