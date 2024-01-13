lazy val `sbt-less` = project in file(".")

enablePlugins(SbtWebBase)

sonatypeProfileName := "com.github.sbt.sbt-less" // See https://issues.sonatype.org/browse/OSSRH-77819#comment-1203625

description := "sbt-web less plugin"

developers += Developer(
  "playframework",
  "The Play Framework Team",
  "contact@playframework.com",
  url("https://github.com/playframework")
)

addSbtJsEngine("1.3.5")
addSbtWeb("1.5.3")

libraryDependencies ++= Seq(
  "org.webjars" % "less-node" % "2.7.2-1",
  "org.webjars" % "mkdirp" % "0.5.0",
  "org.webjars.npm" % "clean-css" % "4.2.4",
  "org.webjars.npm" % "less-plugin-clean-css" % "1.5.1" intransitive(),
  "org.webjars" % "es6-promise-node" % "2.1.1"
)

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverVTagPrefix := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}
