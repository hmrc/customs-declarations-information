import AppDependencies._
import com.typesafe.sbt.web.PathMapping
import com.typesafe.sbt.web.pipeline.Pipeline
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, targetJvm}
import uk.gov.hmrc.gitstamp.GitStampPlugin._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

import java.text.SimpleDateFormat
import java.util.Calendar
import scala.language.postfixOps

name := "customs-declarations-information"
scalaVersion := "2.12.11"
targetJvm := "jvm-1.8"
val silencerVersion = "1.7.0"

lazy val ComponentTest = config("component") extend Test
lazy val CdsIntegrationComponentTest = config("it") extend Test

val testConfig = Seq(ComponentTest, CdsIntegrationComponentTest, Test)

def forkedJvmPerTestConfig(tests: Seq[TestDefinition], packages: String*): Seq[Group] =
  tests.groupBy(_.name.takeWhile(_ != '.')).filter(packageAndTests => packages contains packageAndTests._1) map {
    case (packg, theTests) =>
      Group(packg, theTests, SubProcess(ForkOptions()))
  } toSeq

lazy val testAll = TaskKey[Unit]("test-all")
lazy val allTest = Seq(testAll := (test in ComponentTest)
  .dependsOn((test in CdsIntegrationComponentTest).dependsOn(test in Test)).value)

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .configs(testConfig: _*)
  .settings(
    commonSettings,
    unitTestSettings,
    integrationComponentTestSettings,
    allTest,
    scoverageSettings,
    // Use the silencer plugin to suppress warnings from unused imports in compiled twirl templates
    scalacOptions += "-P:silencer:pathFilters=views;routes",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
  )
  .settings(majorVersion := 0)

lazy val unitTestSettings =
  inConfig(Test)(Defaults.testTasks) ++
    Seq(
      testOptions in Test := Seq(Tests.Filter(unitTestFilter)),
      unmanagedSourceDirectories in Test := Seq((baseDirectory in Test).value / "test"),
      addTestReportOption(Test, "test-reports")
    )

lazy val integrationComponentTestSettings =
  inConfig(CdsIntegrationComponentTest)(Defaults.testTasks) ++
    Seq(
      testOptions in CdsIntegrationComponentTest := Seq(Tests.Filter(integrationComponentTestFilter)),
      parallelExecution in CdsIntegrationComponentTest := false,
      addTestReportOption(CdsIntegrationComponentTest, "int-comp-test-reports"),
      testGrouping in CdsIntegrationComponentTest := forkedJvmPerTestConfig((definedTests in Test).value, "integration", "component")
    )

lazy val commonSettings: Seq[Setting[_]] = publishingSettings ++ gitStampSettings

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := List(
      "<empty>"
      ,"Reverse.*"
      ,"uk\\.gov\\.hmrc\\.customs\\.declarations\\.information\\.upload\\.model\\..*"
      ,"uk\\.gov\\.hmrc\\.customs\\.declarations\\.information\\.views\\..*"
      ,".*(Reverse|AuthService|BuildInfo|Routes).*"
    ).mkString(";"),
  coverageMinimumStmtTotal := 96,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  parallelExecution in Test := false
)

PlayKeys.devSettings := Seq("play.server.http.port" -> "9834")

def integrationComponentTestFilter(name: String): Boolean = (name startsWith "integration") || (name startsWith "component")
def unitTestFilter(name: String): Boolean = name startsWith "unit"

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

val compileDependencies = Seq(customsApiCommon)

val testDependencies = Seq(scalaTestPlusPlay, wireMock, mockito, customsApiCommonTests, flexmark, jacksonModule)

unmanagedResourceDirectories in Compile += baseDirectory.value / "public"
unmanagedResourceDirectories in Test += baseDirectory.value / "test" / "resources"
(managedClasspath in Runtime) += (packageBin in Assets).value

libraryDependencies ++= compileDependencies ++ testDependencies

// Task to create a ZIP file containing all WCO XSDs for each version, under the version directory
val zipWcoXsds = taskKey[Pipeline.Stage]("Zips up all WCO status XSDs and example messages")

zipWcoXsds := { mappings: Seq[PathMapping] =>
  val targetDir = WebKeys.webTarget.value / "zip"
  val zipFiles: Iterable[java.io.File] =
    ((resourceDirectory in Assets).value / "api" / "conf")
      .listFiles
      .filter(_.isDirectory)
      .map { dir =>
        val xsdPaths = Path.allSubpaths(dir / "schemas")
        val exampleMessagesFilter = new SimpleFileFilter(_.getPath.contains("/annotated_XML_samples/"))
        val exampleMessagesPaths = Path.selectSubpaths(dir / "examples", exampleMessagesFilter)
        val zipFile = targetDir / "api" / "conf" / dir.getName / "wco-status-schemas.zip"
        IO.zip(xsdPaths ++ exampleMessagesPaths, zipFile, None)
        val sizeInMb = (BigDecimal(zipFile.length) / BigDecimal(1024 * 1024)).setScale(1, BigDecimal.RoundingMode.UP)
        println(s"Created zip $zipFile")
        val today = Calendar.getInstance().getTime()
        val dateFormat = new SimpleDateFormat("dd/MM/YYYY")
        val lastUpdated = dateFormat.format(today)
        println(s"Update the file size in ${dir.getParent}/${dir.getName}/docs/schema.md to be [ZIP, ${sizeInMb}MB last updated $lastUpdated]")
        println(s"Check the yaml renders correctly file://${dir.getParent}/${dir.getName}/application.yaml")
        println("")
        zipFile
      }
  zipFiles.pair(Path.relativeTo(targetDir)) ++ mappings
}

pipelineStages := Seq(zipWcoXsds)
