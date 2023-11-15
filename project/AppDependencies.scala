import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  val hmrc = "uk.gov.hmrc"
  val hmrcMongo = s"$hmrc.mongo"
  val hmrcMongoVersion = "1.4.0"
  val hmrcBootstrapVersion = "7.23.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    hmrc                %% "bootstrap-backend-play-28"  % hmrcBootstrapVersion,
    hmrc                %% "domain"                     % "8.3.0-play-28",
    hmrc                %% "play-hal"                   % "3.4.0-play-28",
    hmrc                %% "json-encryption"            % "5.2.0-play-28",
    hmrcMongo           %% "hmrc-mongo-play-28"         % hmrcMongoVersion,
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "org.scalatestplus"      %% "mockito-3-4"              % "3.2.1.0"            % scope,
    "org.scalatestplus"      %% "scalacheck-1-17"          % "3.2.16.0"           % scope,
    "com.vladsch.flexmark"   % "flexmark-all"              % "0.64.6"            % scope,
    "org.scalaj"             %% "scalaj-http"              % "2.4.2"              % scope,
    hmrc                     %% "bootstrap-test-play-28"   % hmrcBootstrapVersion % scope,
  )
}
