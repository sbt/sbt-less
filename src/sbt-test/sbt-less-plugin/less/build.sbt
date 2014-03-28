lazy val root = (project in file(".")).addPlugins(SbtWeb)

val checkMapFileContents = taskKey[Unit]("check that map contents are correct")

checkMapFileContents := {
  val contents = IO.read((WebKeys.public in Assets).value / "css" / "a.min.css.map")
  if (contents != """{"version":3,"file":"a.min.css","sources":["a.less"],"names":[],"mappings":"AAAG,GACD"}""") {
    sys.error(s"Unexpected contents: $contents")
  }
}