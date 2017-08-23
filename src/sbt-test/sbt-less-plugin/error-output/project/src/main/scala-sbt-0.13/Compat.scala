import sbt._
import xsbti.{Position, Severity}

import scala.collection.concurrent.TrieMap

object Compat {

  class CapturingLoggerReporter(logger: Logger, errors: TrieMap[String, (Int, Int, String, String)]) extends LoggerReporter(-1, logger) {
    override def log(pos: Position, msg: String, severity: Severity): Unit = {
      errors += (pos.sourceFile().get().getName ->
        (pos.line().get(), pos.offset().get() + 1, msg, pos.lineContent())
      )
    }

  }

}