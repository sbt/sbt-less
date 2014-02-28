sbt-less-plugin
===============

Allows less to be used from within sbt. Builds on com.typesafe.sbt:js-engine in order to execute the less compiler along with
the scripts to verify. js-engine enables high performance linting given parallelism and native JS engine execution.

To use this plugin use the addSbtPlugin command within your project's plugins.sbt (or as a global setting) i.e.:

```scala
addSbtPlugin("com.typesafe" % "sbt-less-plugin" % "1.0.0-M1")
```

Then declare the settings required in your build file (SbtLessPlugin depends on some other, more generalised settings to be
defined). For example, for build.sbt:

```scala
import com.typesafe.sbt.web.SbtWebPlugin
import com.typesafe.sbt.jse.SbtJsTaskPlugin
import com.typesafe.sbt.less.SbtLessPlugin

SbtWebPlugin.webSettings

SbtJsTaskPlugin.jsEngineAndTaskSettings

SbtLessPlugin.lessSettings
```

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
LessKeys.compress in WebKeys.Assets := true
```

&copy; Typesafe Inc., 2013  
