import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

val appName = "individuals-details-api"

lazy val ItTest = config("it") extend Test
lazy val ComponentTest = config("component") extend Test

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
    .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
    .settings(CodeCoverageSettings.settings *)
    .settings(scalaVersion := "3.3.5")
    .settings(scalafmtOnCompile := true)
    .settings(onLoadMessage := "")
    .settings(
      libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test(),
      Test / testOptions := Seq(Tests.Filter((name: String) => name startsWith "unit")),
      routesImport := Seq("uk.gov.hmrc.individualsdetailsapi.Binders._"),
      scalacOptions += "-Wconf:src=routes/.*:s",
      scalacOptions += "-Wconf:msg=unused import&src=html/.*:s",
      scalacOptions += "-Wconf:msg=Flag.*repeatedly:s",
      Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
    )
    .settings(PlayKeys.playDefaultPort := 9655)
    .settings(majorVersion := 0)
    // Disable default sbt Test options (might change with new versions of bootstrap)
    .settings(
      Test / testOptions -= Tests
        .Argument("-o", "-u", "target/test-reports", "-h", "target/test-reports/html-report")
    )
    // Suppress successful events in Scalatest in standard output (-o)
    // Options described here: https://www.scalatest.org/user_guide/using_scalatest_with_sbt
    .settings(
      Test / testOptions += Tests.Argument(
        TestFrameworks.ScalaTest,
        "-oNCHPQR",
        "-u",
        "target/test-reports",
        "-h",
        "target/test-reports/html-report"
      )
    )
    .configs(ItTest)
    .settings(inConfig(ItTest)(Defaults.testSettings) *)
    .settings(
      ItTest / unmanagedSourceDirectories := (ItTest / baseDirectory)(base => Seq(base / "test")).value,
      ItTest / testOptions := Seq(Tests.Filter((name: String) => name startsWith "it")),
      // Disable default sbt Test options (might change with new versions of bootstrap)
      ItTest / testOptions -= Tests
        .Argument("-o", "-u", "target/int-test-reports", "-h", "target/int-test-reports/html-report"),
      ItTest / testOptions += Tests.Argument(
        TestFrameworks.ScalaTest,
        "-oNCHPQR",
        "-u",
        "target/int-test-reports",
        "-h",
        "target/int-test-reports/html-report"
      )
    )
    .configs(ComponentTest)
    .settings(inConfig(ComponentTest)(Defaults.testSettings) *)
    .settings(
      ComponentTest / testOptions := Seq(Tests.Filter((name: String) => name startsWith "component")),
      ComponentTest / unmanagedSourceDirectories := (ComponentTest / baseDirectory)(base => Seq(base / "test")).value,
      // Disable default sbt Test options (might change with new versions of bootstrap)
      ComponentTest / testOptions -= Tests
        .Argument("-o", "-u", "target/component-test-reports", "-h", "target/component-test-reports/html-report"),
      ComponentTest / testOptions += Tests.Argument(
        TestFrameworks.ScalaTest,
        "-oNCHPQR",
        "-u",
        "target/component-test-reports",
        "-h",
        "target/component-test-reports/html-report"
      )
    )
