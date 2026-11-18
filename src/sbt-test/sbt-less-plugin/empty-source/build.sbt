lazy val root = (project in file(".")).enablePlugins(SbtWeb)

(Assets / LessKeys.less / includeFilter) := "empty.less"

val checkFileContents = taskKey[Unit]("Check for emptiness")

checkFileContents := {
  val contents = IO.read((Assets / WebKeys.public).value / "css" / "empty.css")

  if (contents.nonEmpty) {
    sys.error(s"Output should be empty, but got '$contents'")
  }
}

ThisBuild / scalaVersion := {
  if (sbtVersion.value.startsWith("1.")) "2.12.21" else "3.9.0"
}
