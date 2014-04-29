lazy val root = (project in file(".")).addPlugins(SbtWeb)

val checkMapFileContents = taskKey[Unit]("check that map contents are correct")

checkMapFileContents := {
  val contents = IO.read((WebKeys.public in Assets).value / "css" / "main.min.css.map")
  if (contents != """{"version":3,"file":"main.min.css","sources":["main.less"],"names":[],"mappings":"AAAG,GACD"}""") {
    sys.error(s"Unexpected contents: $contents")
  }
}