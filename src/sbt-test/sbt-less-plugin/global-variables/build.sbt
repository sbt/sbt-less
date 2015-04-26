lazy val root = (project in file(".")).enablePlugins(SbtWeb)

LessKeys.globalVariables := Seq("color" -> "blue")

val checkFileContents = taskKey[Unit]("Check for the presence of a variable")

checkFileContents := {
  val contents = IO.read((WebKeys.public in Assets).value / "css" / "main.css")
  val expected = "color: blue"

  if (!contents.contains(expected)) {
    sys.error(s"Output did not contain '$expected':\n $contents")
  }
}
