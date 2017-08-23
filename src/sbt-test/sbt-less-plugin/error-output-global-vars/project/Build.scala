import com.typesafe.sbt.jse.JsEngineImport.JsEngineKeys.EngineType
import sbt._
import scala.sys.process.Process

object ErrorBuild {

  def maybeNode: EngineType.Value = {
    try {
      val version = Process("node --version").!!
      println(s"Found node $version")
      EngineType.Node
    } catch {
      case e: Exception =>
        println("node not found, falling back to trireme")
        EngineType.Trireme
    }
  }
}