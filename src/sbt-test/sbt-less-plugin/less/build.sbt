import com.typesafe.sbt.jse.SbtJsTaskPlugin

webSettings

SbtJsTaskPlugin.jsEngineAndTaskSettings

lessSettings

val checkMapFileContents = taskKey[Unit]("check that map contents are correct")

checkMapFileContents := {
  val contents = IO.read((WebKeys.public in WebKeys.Assets).value / "css" / "a.min.css.map")
  if (contents != """{"version":3,"file":"a.min.css","sources":["css/a.less"],"names":[],"mappings":"AAAG,GACD"}""") {
    sys.error(s"Unexpected contents: $contents")
  }
}