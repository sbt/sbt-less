import sbt.Level
import xsbti.Problem
import sbt.internal.inc.LoggedReporter
import sbt.Logger
import scala.collection.concurrent.TrieMap

object Compat {
  class CapturingLoggerReporter(logger: Logger, errors: TrieMap[String, (Int, Int, String, String)]) extends LoggedReporter(-1, logger) {
    override def log(problem: Problem): Unit = {
      errors += (problem.position().sourceFile().get().getName ->
        (problem.position().line().get(), problem.position().offset().get() + 1, problem.message(), problem.position().lineContent())
      )
    }
  }
}