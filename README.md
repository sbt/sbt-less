less-sbt-plugin
===============

Allows jslint to be used from within sbt. Builds on com.typesafe:webdriver in order to execute jslint.js
along with the scripts to verify. WebDriver enables high performance linting given parallelism and native
browser execution.

To use this plugin use the addSbtPlugin command within your project's plugins.sbt (or as a global setting). Then
declare the settings required in your build file. For example, for build.sbt:

    lessSettings

&copy; Typesafe Inc., 2013
