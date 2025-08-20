import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

val appName = "individuals-details-api"

lazy val ItTest = config("it") extend Test
lazy val ComponentTest = config("component") extend Test

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
    .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
    .settings(CodeCoverageSettings.settings*)
    .settings(scalaVersion := "3.7.1")
    .settings(scalafmtOnCompile := true)
    .settings(onLoadMessage := "")
    .settings(
      libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test(),
      Test / testOptions := Seq(Tests.Filter((name: String) => name startsWith "unit")),
      routesImport := Seq("uk.gov.hmrc.individualsdetailsapi.Binders._"),
      scalacOptions ++= Seq(
        "-Wconf:src=routes/.*:s,src=txt/.*:s",
        "-Wconf:msg=Flag.*repeatedly:s"
      ),
      Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
    )
    .settings(PlayKeys.playDefaultPort := 9655)
    .settings(majorVersion := 0)
    .settings(
      Test / testOptions += Tests.Argument(
        TestFrameworks.ScalaTest,
        "-u",
        "target/test-reports",
        "-h",
        "target/test-reports/html-report"
      )
    )
    .configs(ItTest)
    .settings(inConfig(ItTest)(Defaults.testSettings)*)
    .settings(
      ItTest / unmanagedSourceDirectories := (ItTest / baseDirectory)(base => Seq(base / "test")).value,
      ItTest / testOptions := Seq(Tests.Filter((name: String) => name startsWith "it")),
      ItTest / testOptions += Tests.Argument(
        TestFrameworks.ScalaTest,
        "-u",
        "target/int-test-reports",
        "-h",
        "target/int-test-reports/html-report"
      )
    )
    .configs(ComponentTest)
    .settings(inConfig(ComponentTest)(Defaults.testSettings)*)
    .settings(
      ComponentTest / testOptions := Seq(Tests.Filter((name: String) => name startsWith "component")),
      ComponentTest / unmanagedSourceDirectories := (ComponentTest / baseDirectory)(base => Seq(base / "test")).value,
      ComponentTest / testOptions += Tests.Argument(
        TestFrameworks.ScalaTest,
        "-u",
        "target/component-test-reports",
        "-h",
        "target/component-test-reports/html-report"
      )
    )
