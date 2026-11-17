lazy val root = (project in file(".")).enablePlugins(SbtWeb)

libraryDependencies += "org.webjars" % "bootstrap" % "3.0.2"

LessKeys.relativeImports := false

val checkMapFileContents = taskKey[Unit]("check that map contents are correct")

checkMapFileContents := {
  var re = """^.+,"sources":\["main\.less","[^"]+/sbt_[^"]+/target/(out/jvm/scala-[.0-9]+/root/)?web/web-modules/main/webjars/lib/bootstrap/less/mixins\.less"\],.+$""".r
  val contents = IO.read((Assets / WebKeys.public).value / "main.css.map")
  if (!re.pattern.matcher(contents).matches) {
    sys.error(s"Unexpected contents: $contents")
  }
}

ThisBuild / scalaVersion := {
  if (sbtVersion.value.startsWith("1.")) "2.12.21" else "3.9.0"
}
