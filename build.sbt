import sbt.Keys.compile
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import play.sbt.routes.RoutesKeys

val appName = "individuals-details-api"

TwirlKeys.templateImports := Seq.empty
RoutesKeys.routesImport := Seq("uk.gov.hmrc.individualsdetailsapi.Binders._")

def intTestFilter(name: String): Boolean = name startsWith "it"
def unitFilter(name: String): Boolean = name startsWith "unit"
def componentFilter(name: String): Boolean = name startsWith "component"

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
    .settings(scalaSettings: _*)
    .settings(CodeCoverageSettings.settings *)
    .settings(ThisBuild / useSuperShell := false)
    .settings(scalaVersion := "2.13.8")
    .settings(defaultSettings(): _*)
    .settings(onLoadMessage := "")
    .settings(
      libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test(),
      Test / testOptions := Seq(Tests.Filter(unitFilter)),
      retrieveManaged := true,
    )
    .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "resources")
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(
      IntegrationTest / Keys.fork := false,
      IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(
        base => Seq(base / "test")).value,
      IntegrationTest / testOptions := Seq(Tests.Filter(intTestFilter)),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      IntegrationTest / testGrouping := oneForkedJvmPerTest(
        (IntegrationTest / definedTests).value),
      IntegrationTest / parallelExecution := false,
      // Disable default sbt Test options (might change with new versions of bootstrap)
      IntegrationTest / testOptions -= Tests
        .Argument("-o", "-u", "target/int-test-reports", "-h", "target/int-test-reports/html-report"),
      IntegrationTest / testOptions += Tests.Argument(
        TestFrameworks.ScalaTest,
        "-oNCHPQR",
        "-u",
        "target/int-test-reports",
        "-h",
        "target/int-test-reports/html-report")
    )
    .configs(ComponentTest)
    .settings(inConfig(ComponentTest)(Defaults.testSettings): _*)
    .settings(
      scalacOptions += "-Wconf:src=routes/.*:s",
      scalacOptions += "-Wconf:cat=unused-imports&src=txt/.*:s",
      ComponentTest / testOptions := Seq(Tests.Filter(componentFilter)),
      ComponentTest / unmanagedSourceDirectories := (ComponentTest / baseDirectory)(
        base => Seq(base / "test")).value,
      ComponentTest / testGrouping := oneForkedJvmPerTest(
        (ComponentTest / definedTests).value),
      ComponentTest / parallelExecution := false,
      // Disable default sbt Test options (might change with new versions of bootstrap)
      ComponentTest / testOptions -= Tests
        .Argument("-o", "-u", "target/component-test-reports", "-h", "target/component-test-reports/html-report"),
      ComponentTest / testOptions += Tests.Argument(
        TestFrameworks.ScalaTest,
        "-oNCHPQR",
        "-u",
        "target/component-test-reports",
        "-h",
        "target/component-test-reports/html-report")
    )
    .settings(resolvers ++= Seq(
      Resolver.jcenterRepo
    ))
    .settings(PlayKeys.playDefaultPort := 9655)
    .settings(majorVersion := 0)
    // Disable default sbt Test options (might change with new versions of bootstrap)
    .settings(Test / testOptions -= Tests
      .Argument("-o", "-u", "target/test-reports", "-h", "target/test-reports/html-report"))
    // Suppress successful events in Scalatest in standard output (-o)
    // Options described here: https://www.scalatest.org/user_guide/using_scalatest_with_sbt
    .settings(Test / testOptions += Tests.Argument(
      TestFrameworks.ScalaTest,
      "-oNCHPQR",
      "-u",
      "target/test-reports",
      "-h",
      "target/test-reports/html-report"))

lazy val ComponentTest = config("component") extend Test

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests.map { test =>
    new Group(
      test.name,
      Seq(test),
      SubProcess(
        ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }

lazy val compileAll = taskKey[Unit]("Compiles sources in all configurations.")

compileAll := {
  val a = (Test / compile).value
  val b = (IntegrationTest / compile).value
  ()
}
