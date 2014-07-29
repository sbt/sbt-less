lazy val main = (project in file("."))
  .enablePlugins(SbtWeb)
  .dependsOn(a)

lazy val a = (project in file("modules/a"))
  .enablePlugins(SbtWeb)
  .dependsOn(b)

lazy val b = (project in file("modules/b"))
  .enablePlugins(SbtWeb)
