sbtPlugin := true

organization := "com.typesafe"

name := "sbt-less-plugin"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "org.webjars" % "less" % "1.5.1-SNAPSHOT",
  "org.webjars" % "source-map" % "0.1.31-SNAPSHOT",
  "org.webjars" % "webjars-locator" % "0.7-SNAPSHOT",
  "com.typesafe" %% "jse" % "1.0.0-SNAPSHOT",
  "org.specs2" %% "specs2" % "2.2.2" % "test",
  "junit" % "junit" % "4.11" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test"
)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

addSbtPlugin("com.typesafe" %% "sbt-js-engine" % "1.0.0-SNAPSHOT")

scriptedSettings

scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }
