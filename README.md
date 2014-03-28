sbt-less
========

[![Build Status](https://api.travis-ci.org/sbt/sbt-less.png?branch=master)](https://travis-ci.org/sbt/sbt-less)

Allows less to be used from within sbt. Builds on com.typesafe.sbt:js-engine in order to execute the less compiler along with
the scripts to verify. js-engine enables high performance linting given parallelism and native JS engine execution.

To use this plugin use the addSbtPlugin command within your project's plugins.sbt (or as a global setting) i.e.:

```scala
resolvers ++= Seq(
    Resolver.url("sbt snapshot plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
    Resolver.sonatypeRepo("snapshots"),
    "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
    )

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0-SNAPSHOT")
```

Your project's build file also needs to enable sbt-web plugins. For example with build.sbt:

    lazy val root = (project in file(".")).addPlugins(SbtWeb)

The compiler allows most of the same options to be specified as the (lessc CLI itself)[http://lesscss.org/usage/].
Here are the options:

Option              | Description
--------------------|------------
cleancss            | Compress output using clean-css.
cleancssOptions     | Pass an option to clean css, using CLI arguments from https://github.com/GoalSmashers/clean-css .
color               | Whether LESS output should be colorised
compress            | Compress output by removing some whitespaces.
ieCompat            | Do IE compatibility checks.
insecure            | Allow imports from insecure https hosts.
maxLineLen          | Maximum line length.
optimization        | Set the parser's optimization level.
relativeUrls        | Re-write relative urls to the base less file.
rootpath            | Set rootpath for url rewriting in relative imports and urls.
silent              | Suppress output of error messages.
sourceMap           | Outputs a v3 sourcemap.
sourceMapFileInline | Whether the source map should be embedded in the output file
sourceMapLessInline | Whether to embed the less code in the source map
sourceMapRootpath   | Adds this path onto the sourcemap filename and less file paths.
strictImports       | Whether imports should be strict.
strictMath          | Requires brackets. This option may default to true and be removed in future.
strictUnits         | Whether all unit should be strict, or if mixed units are allowed.
verbose             | Be verbose.
    
The following sbt code illustrates how compression can be enabled:

```scala
LessKeys.compress in Assets := true
```

&copy; Typesafe Inc., 2013  
