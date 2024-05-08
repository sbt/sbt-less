lazy val root = (project in file(".")).enablePlugins(SbtWeb)

val checkMapFileContents = taskKey[Unit]("check that map contents are correct")

checkMapFileContents := {
  val contents = IO.read((Assets / WebKeys.public).value / "css" / "main.min.css.map")
  val expectedContents = """{"version":3,"sources":["main.less"],"names":[],"mappings":"AAGA,GACE,UAAA,CACA,IAAK","file":"main.min.css"}"""

  if (contents != expectedContents) {
    sys.error(s"Unexpected contents: $contents, \nexpected: $expectedContents")
  }
}
