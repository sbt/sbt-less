lazy val root = (project in file(".")).enablePlugins(SbtWeb)

val checkMapFileContents = taskKey[Unit]("check that map contents are correct")

checkMapFileContents := {
  val contents = IO.read((WebKeys.public in Assets).value / "css" / "main.min.css.map")
  val expectedContents = """{"version":3,"sources":["main.less"],"names":[],"mappings":"AAAA,GACE","file":"main.min.css"}"""

  if (contents != expectedContents) {
    sys.error(s"Unexpected contents: $contents, \nexpected: $expectedContents")
  }
}
