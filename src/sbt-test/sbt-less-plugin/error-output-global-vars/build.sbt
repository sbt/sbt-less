import LessKeys._
import JsEngineKeys._
import scala.collection.concurrent.TrieMap

lazy val root = (project in file("."))
  .enablePlugins(SbtWeb)

LessKeys.globalVariables := Seq("padding" -> "10px")

// Map of file name to line, column, error message, line contents
val errors = SettingKey[TrieMap[String, (Int, Int, String, String)]]("errors")

errors := TrieMap.empty

WebKeys.reporter := new Compat.CapturingLoggerReporter(streams.value.log, errors.value)

(Assets / less / includeFilter) := GlobFilter("*.less")

InputKey[Unit]("errorExists") := {
  val args = Def.spaceDelimited("<file> <line> <column>").parsed
  val file = args(0)
  val line = args(1).toInt
  val col = args(2).toInt
  val theErrors = errors.value
  theErrors.get(file) match {
    case Some((l, c, msg, contents)) =>
      if (line != l) {
        throw new RuntimeException(s"Error in $file expected on line $line but was found on $l: $msg\n$contents\nAll errors: $theErrors")
      }
      if (col != c) {
        throw new RuntimeException(s"Error in $file:$line expected on column $col but was found on $c: $msg\n$contents\nAll errors: $theErrors")
      }
    case None =>
      throw new RuntimeException(s"Error expected in $file but no error found.\nAll errors: $theErrors")
  }
}

InputKey[Unit]("errorMsg") := {
  val args = Def.spaceDelimited("<file> <msg>").parsed
  val file = args(0)
  val msg = args.tail.mkString(" ")
  val theErrors = errors.value
  theErrors.get(file) match {
    case Some((_, _, err, _)) =>
      if (err != msg) {
        throw new RuntimeException(s"Error in $file expected '$msg' but was '$err'")
      }
    case None =>
      throw new RuntimeException(s"Error message expected in $file but no error found ($theErrors)")
  }
}

InputKey[Unit]("errorContents") := {
  val args = Def.spaceDelimited("<file> <contents>").parsed
  val file = args(0)
  val contents = args.tail.mkString(" ")
  val theErrors = errors.value
  theErrors.get(file) match {
    case Some((_, _, _, c)) =>
      if (contents != c.trim) {
        throw new RuntimeException(s"Error in $file expected contents '$contents' but was '$c'")
      }
    case None =>
      throw new RuntimeException(s"Error contents expected in $file but no error found ($theErrors)")
  }
}

InputKey[Unit]("resetErrors") := {
  errors.value.clear()
}
