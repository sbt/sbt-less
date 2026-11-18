lazy val root = (project in file(".")).enablePlugins(SbtWeb)

InputKey[Unit]("contains") := {
  val Seq(pattern, expect) = Def.spaceDelimited("<file-pattern> <expect>").parsed

  val matches: Seq[File] = sbt.nio.file.FileTreeView.default
    .list(Glob(baseDirectory.value.toPath, pattern))
    .map { case (path, _) => path.toFile }

  matches.headOption match {
    case Some(f) =>
      val contents = IO.read(f)
      if (!contents.contains(expect)) {
        sys.error(s"${f.getPath} does not contain $expect, instead was: $contents")
      }
    case None =>
      sys.error(s"No files matched pattern: $pattern")
  }
}

ThisBuild / scalaVersion := {
  if (sbtVersion.value.startsWith("1.")) "2.12.21" else "3.9.0"
}
