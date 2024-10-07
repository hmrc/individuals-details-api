import sbt.*

object AppDependencies {
  val hmrc = "uk.gov.hmrc"
  val playVersion = "play-30"
  val hmrcMongo = s"$hmrc.mongo"
  val hmrcMongoVersion = "1.7.0"
  val hmrcBootstrapVersion = "9.0.0"

  val compile: Seq[ModuleID] = Seq(
    hmrc      %% s"bootstrap-backend-$playVersion" % hmrcBootstrapVersion,
    hmrc      %% s"domain-$playVersion"            % "9.0.0",
    hmrc      %% s"play-hal-$playVersion"          % "4.0.0",
    hmrc      %% s"crypto-json-$playVersion"       % "7.6.0",
    hmrcMongo %% s"hmrc-mongo-$playVersion"        % hmrcMongoVersion
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "org.scalatestplus" %% "mockito-3-4"                     % "3.2.1.0"            % scope,
    "org.scalatestplus" %% "scalacheck-1-17"                 % "3.2.16.0"           % scope,
    "org.scalaj"        %% "scalaj-http"                     % "2.4.2"              % scope,
    hmrc                %% s"bootstrap-test-$playVersion"    % hmrcBootstrapVersion % scope
  )
}
