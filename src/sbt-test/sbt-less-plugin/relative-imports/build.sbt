lazy val root = (project in file(".")).enablePlugins(SbtWeb)

libraryDependencies += "org.webjars" % "bootstrap" % "3.0.2"

val checkMapFileContents = taskKey[Unit]("check that map contents are correct")

checkMapFileContents := {
  val contents = IO.read((WebKeys.public in Assets).value / "main.css.map")
  if (contents != """{"version":3,"file":"main.css","sources":["main.less","../lib/bootstrap/less/mixins.less"],"names":[],"mappings":"AAEA;ECsCE,cAAA;EACA,iBAAA;EACA,kBAAA"}""") {
    sys.error(s"Unexpected contents: $contents")
  }
}
