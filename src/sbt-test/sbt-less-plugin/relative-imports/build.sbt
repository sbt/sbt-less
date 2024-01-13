lazy val root = (project in file(".")).enablePlugins(SbtWeb)

libraryDependencies += "org.webjars" % "bootstrap" % "3.0.2"

val checkMapFileContents = taskKey[Unit]("check that map contents are correct")

checkMapFileContents := {
  val contents = IO.read((Assets / WebKeys.public).value / "main.css.map")
  val expectedContents = """{"version":3,"sources":["main.less","../lib/bootstrap/less/mixins.less"],"names":[],"mappings":"AAEA;ECsCE,cAAA;EACA,iBAAA;EACA,kBAAA","file":"main.css"}"""

  if (contents != expectedContents) {
    sys.error(s"Unexpected contents: $contents, \nexpected: $expectedContents")
  }
}
