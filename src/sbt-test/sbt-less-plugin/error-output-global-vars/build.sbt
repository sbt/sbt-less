import LessKeys._
import JsEngineKeys._
import scala.collection.concurrent.TrieMap

lazy val root = (project in file("."))
  .enablePlugins(SbtWeb)

LessKeys.globalVariables := Seq("padding" -> "10px")

// Map of file name to line, column, error message, line contents
val errors = SettingKey[TrieMap[String, (Int, Int, String, String)]]("errors")

errors := TrieMap.empty

WebKeys.reporter := new LoggerReporter(-1, new Logger {
  var currentError: Option[(String, Int, String)] = None
  var currentContents: Option[String] = None
  val Error = """.*/([^/]+\.less):(\d+): (.*)""".r
  def trace(t: => Throwable): Unit = {}
  def success(message: => String): Unit = {}
  def log(level: Level.Value, message: => String): Unit = {
    if (level == Level.Error) {
      (currentError, currentContents, message) match {
        case (None, None, Error(file, line, msg)) =>
          currentError = Some((file, line.toInt, msg))
        case (Some(_), None, msg) =>
          currentContents = Some(msg)
        case (Some((file, line, err)), Some(contents), msg) =>
          errors.value += (file -> (line, msg.indexOf('^') + 1, err, contents))
          currentError = None
          currentContents = None
        case _ =>
      }
    }
  }
})

includeFilter in (Assets, less) := GlobFilter("*.less")

InputKey[Unit]("error-exists") := {
  val args = Def.spaceDelimited("<file> <line> <column>").parsed
  val file = args(0)
  val line = args(1).toInt
  val col = args(2).toInt
  errors.value.get(file) match {
    case Some((l, c, msg, contents)) =>
      if (line != l) {
        throw new RuntimeException(s"Error in $file expected on line $line but was found on $l: $msg\n$contents\nAll errors: ${errors.value}")
      }
      if (col != c) {
        throw new RuntimeException(s"Error in $file:$line expected on column $col but was found on $c: $msg\n$contents\nAll errors: ${errors.value}")
      }
    case None =>
      throw new RuntimeException(s"Error expected in $file but no error found.\nAll errors: ${errors.value}")
  }
}

InputKey[Unit]("error-msg") := {
  val args = Def.spaceDelimited("<file> <msg>").parsed
  val file = args(0)
  val msg = args.tail.mkString(" ")
  errors.value.get(file) match {
    case Some((_, _, err, _)) =>
      if (err != msg) {
        throw new RuntimeException(s"Error in $file expected '$msg' but was '$err'")
      }
    case None =>
      throw new RuntimeException(s"Error message expected in $file but no error found (${errors.value})")
  }
}

InputKey[Unit]("error-contents") := {
  val args = Def.spaceDelimited("<file> <contents>").parsed
  val file = args(0)
  val contents = args.tail.mkString(" ")
  errors.value.get(file) match {
    case Some((_, _, _, c)) =>
      if (contents != c.trim) {
        throw new RuntimeException(s"Error in $file expected contents '$contents' but was '$c'")
      }
    case None =>
      throw new RuntimeException(s"Error contents expected in $file but no error found (${errors.value})")
  }
}

InputKey[Unit]("reset-errors") := {
  errors.value.clear()
}