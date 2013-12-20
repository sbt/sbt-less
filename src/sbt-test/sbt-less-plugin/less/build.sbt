import com.typesafe.web.sbt.WebPlugin._

webSettings

JsEngineKeys.engineType in GlobalScope := JsEngineKeys.EngineType.CommonNode

lessSettings