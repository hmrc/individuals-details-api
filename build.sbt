import sbt.Keys.compile
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import play.sbt.routes.RoutesKeys

val appName = "individuals-details-api"

TwirlKeys.templateImports := Seq.empty
RoutesKeys.routesImport := Seq("uk.gov.hmrc.individualsdetailsapi.Binders._")

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;uk.gov.hmrc.individualsdetailsapi.views.*;" +
      ".*BuildInfo.;uk.gov.hmrc.BuildInfo;.*Routes;.*RoutesPrefix*;",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

def intTestFilter(name: String): Boolean = name startsWith "it"
def unitFilter(name: String): Boolean = name startsWith "unit"
def componentFilter(name: String): Boolean = name startsWith "component"

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
    .settings(scalaSettings: _*)
    .settings(scoverageSettings: _*)
    .settings(useSuperShell in ThisBuild := false)
    .settings(scalaVersion := "2.13.8")
    .settings(defaultSettings(): _*)
    .settings(onLoadMessage := "")
    .settings(
      libraryDependencies ++= (AppDependencies.compile ++ AppDependencies
        .test()),
      Test / testOptions := Seq(Tests.Filter(unitFilter)),
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default
        .withWarnScalaVersionEviction(false)
    )
    .settings(unmanagedResourceDirectories in Compile += baseDirectory.value / "resources")
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(
      Keys.fork in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(
        base => Seq(base / "test")).value,
      testOptions in IntegrationTest := Seq(Tests.Filter(intTestFilter)),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := oneForkedJvmPerTest(
        (definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false
    )
    .configs(ComponentTest)
    .settings(inConfig(ComponentTest)(Defaults.testSettings): _*)
    .settings(
      scalacOptions += "-Wconf:src=routes/.*:s",
      scalacOptions += "-Wconf:cat=unused-imports&src=txt/.*:s",
      ComponentTest / testOptions := Seq(Tests.Filter(componentFilter)),
      ComponentTest / unmanagedSourceDirectories := (baseDirectory in ComponentTest)(
        base => Seq(base / "test")).value,
      testGrouping in ComponentTest := oneForkedJvmPerTest(
        (definedTests in ComponentTest).value),
      parallelExecution in ComponentTest := false
    )
    .settings(resolvers ++= Seq(
      Resolver.jcenterRepo
    ))
    .settings(PlayKeys.playDefaultPort := 9655)
    .settings(majorVersion := 0)

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
  val a = (compile in Test).value
  val b = (compile in IntegrationTest).value
  ()
}
