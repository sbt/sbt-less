lazy val root = (project in file(".")).enablePlugins(SbtWeb)

InputKey[Unit]("contains") := {
  val Seq(checkFile, expect) = Def.spaceDelimited("<file> <expect>").parsed
  val contents = IO.read(file(checkFile))
  if (!contents.contains(expect)) {
    sys.error(s"$checkFile does not contain $expect, instead was: $contents")
  }
}