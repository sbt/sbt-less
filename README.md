less-sbt-plugin
===============

Allows less to be used from within sbt. Builds on com.typesafe:js-engine in order to execute the less compiler along with the scripts to verify. js-engine enables high performance linting given parallelism and native JS engine execution.

To use this plugin use the addSbtPlugin command within your project's plugins.sbt (or as a global setting) i.e.:

    resolvers ++= Seq(
        Resolver.url("sbt snapshot plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
        Resolver.sonatypeRepo("snapshots"),
        "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
        )

    addSbtPlugin("com.typesafe" % "sbt-less-plugin" % "1.0.0-SNAPSHOT")

Then declare the settings required in your build file (LessPlugin depends on some other, more generalised settings to be defined). For example, for build.sbt:

    import com.typesafe.web.sbt.WebPlugin
    import com.typesafe.jse.sbt.JsEnginePlugin
    import com.typesafe.jshint.sbt.LessPlugin

    WebPlugin.webSettings

    JsEnginePlugin.jsEngineSettings

    LessPlugin.lessSettings

The compiler allows most of the same options to be specified as the less compiler itself.  A complete list of options are:

* *silent*: Suppress output of error messages. Defaults to false.
* *verbose*: Be verbose. Defaults to false.
* *ieCompat*: Do IE compatibility checks. Defaults to true.
* *compress*: Compress output by removing some whitespaces. Defaults to false.
* *cleancss*: Compress output using clean-css. Defaults to false.
* *includePaths*: The include paths to search when looking for LESS imports.
* *sourceMap*: Outputs a v3 sourcemap. Defaults to true.
* *sourceMapLessInline*: Whether to embed the less code in the source map. Defaults to false.
* *sourceMapFileInline*: Whether the source map should be embedded in the output file. Defaults to false.
* *sourceMapRootpath*: Adds this path onto the sourcemap filename and less file paths.
* *maxLineLen*: Maximum line length. Defaults to no maximum.
* *strictMath*: Requires brackets. This option may default to true and be removed in future. Defaults to false.
* *strictUnits*: Whether all unit should be strict, or if mixed units are allowed. Defaults to false.
* *strictImports*: Whether imports should be strict. Defaults to false.
* *optimization*: Set the parser's optimization level. Defaults to 1.
* *color*: Whether LESS output should be colorised. Defaults to whether SBT coloring is enabled or not.
* *insecure*: Allow imports from insecure https hosts. Defaults to false.
* *rootpath*: Set rootpath for url rewriting in relative imports and urls.
* *relativeUrls*: Re-write relative urls to the base less file. Defaults to false.

At present the only supported engine is [Node](http://nodejs.org/).
In-jvm options should become available soon. To use Node declare the following in your build file:

    import com.typesafe.jse.sbt.JsEnginePlugin.JsEngineKeys

    JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

node is required to be available on your shell's path in order for it to be used. To check for its availability simply type `node`.

&copy; Typesafe Inc., 2013  
