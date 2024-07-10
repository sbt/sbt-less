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

addSbtJsEngine("1.3.9")
addSbtWeb("1.5.6")

libraryDependencies ++= Seq(
  "org.webjars.npm" % "node-require-fallback" % "1.0.0",
  "org.webjars.npm" % "less" % "4.2.0", // sync with src/main/resources/lessc.js
  "org.webjars.npm" % "clone" % "2.1.2",
  "org.webjars.npm" % "mkdirp" % "0.5.6", // sync with src/main/resources/lessc.js
  "org.webjars.npm" % "clean-css" % "5.3.2",
  "org.webjars.npm" % "less-plugin-clean-css" % "1.5.1" intransitive(), // sync with src/main/resources/lessc.js
  "org.webjars.npm" % "es6-promise" % "4.2.8", // sync with src/main/resources/lessc.js
)

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverVTagPrefix := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}
