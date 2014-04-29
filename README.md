sbt-less
========

[![Build Status](https://api.travis-ci.org/sbt/sbt-less.png?branch=master)](https://travis-ci.org/sbt/sbt-less)

Allows less to be used from within sbt. Builds on com.typesafe.sbt:js-engine in order to execute the less compiler along with
the scripts to verify. js-engine enables high performance linting given parallelism and native JS engine execution.

To use this plugin use the addSbtPlugin command within your project's plugins.sbt (or as a global setting) i.e.:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0-M2a")
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

By default only `main.less` is looked for given that the LESS compiler must be explicitly fed the files
that are required for compilation. Beyond just `main.less`, you can use an expression in your `build.sbt` like the
following:

```scala
includeFilter in (Assets, LessKeys.less) := "foo.less" | "bar.less"
```

...where both `foo.less` and `bar.less` will be considered for the LESS compiler.

Alternatively you may want a more general expression to exclude LESS files that are not considered targets
for the compiler. Quite commonly, LESS files are divided up into those entry point files and other files, with the
latter set intended for importing into the entry point files. These other files tend not to be suitable for the
compiler in isolation as they can depend on the global declarations made by other non-imported LESS files. For example,
you may have a convention where any LESS file starting with an `_` should not be considered for direct compilation. To
include all `.less` files but exclude any beginning with an `_` you can use the following declaration:

```scala
includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := new PatternFilter("""[_].*\.less""".r.pattern)
```

&copy; Typesafe Inc., 2013, 2014
