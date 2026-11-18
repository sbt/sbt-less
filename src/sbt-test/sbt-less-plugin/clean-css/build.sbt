lazy val root = (project in file(".")).enablePlugins(SbtWeb)

LessKeys.cleancss := true

val checkCleanCssUsed = taskKey[Unit]("check that clean-css has been used")

checkCleanCssUsed := {
  val contents = IO.read((Assets / WebKeys.public).value / "css" / "main.css")
  val expectedContents = """h1{color:#00f}/*# sourceMappingURL=main.css.map */"""

  if (contents != expectedContents) {
    sys.error(s"Unexpected contents: $contents, \nexpected: $expectedContents")
  }
}

ThisBuild / scalaVersion := {
  if (sbtVersion.value.startsWith("1.")) "2.12.21" else "3.9.0"
}
