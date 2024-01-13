lazy val plugin = RootProject(file("..").getAbsoluteFile.toURI)

lazy val root = (project in file(".")).dependsOn(plugin)
