import com.typesafe.sbt.web.PathMapping
import com.typesafe.sbt.web.pipeline.Pipeline
import play.sbt.PlayImport.PlayKeys.playDefaultPort
import sbt.*
import sbt.Keys.*
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.addTestReportOption
import uk.gov.hmrc.gitstamp.GitStampPlugin.*
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

import java.text.SimpleDateFormat
import java.util.Calendar
import scala.language.postfixOps

name := "customs-declarations-information"
ThisBuild/scalaVersion := "3.3.5"
//Test / fork := false

lazy val ComponentTest = config("it/test/component") extend Test
lazy val CdsIntegrationComponentTest = config("it") extend Test

val testConfig = Seq(ComponentTest, CdsIntegrationComponentTest, Test)

def forkedJvmPerTestConfig(tests: Seq[TestDefinition], packages: String*): Seq[Group] =
  tests.groupBy(_.name.takeWhile(_ != '.')).filter(packageAndTests => packages contains packageAndTests._1) map {
    case (packg, theTests) =>
      Group(packg, theTests, SubProcess(ForkOptions()))
  } toSeq

lazy val testAll = TaskKey[Unit]("test-all")
lazy val allTest = Seq(testAll := (ComponentTest / test)
  .dependsOn((CdsIntegrationComponentTest / test).dependsOn(Test / test)).value)

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .configs(testConfig: _*)
  .settings(
    commonSettings,
    unitTestSettings,
    allTest,
    scoverageSettings
  )
  .settings(majorVersion := 0)
  .settings(playDefaultPort := 9834)

lazy val unitTestSettings =
  inConfig(Test)(Defaults.testTasks) ++
    Seq(
      Test / testOptions := Seq(Tests.Filter(unitTestFilter)),
      Test / unmanagedSourceDirectories := Seq((Test / baseDirectory).value / "test"),
      addTestReportOption(Test, "test-reports")
    )

lazy val it = project.in(file("it"))
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .enablePlugins(play.sbt.PlayScala)
  .settings(majorVersion := 0)

lazy val commonSettings: Seq[Setting[_]] = gitStampSettings

ThisBuild/scalacOptions ++= Seq("-Wconf:src=routes/.*:s",
  "-Wconf:msg=Flag.*repeatedly:s")

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := List(
    "<empty>"
    ,"Reverse.*"
    ,"uk\\.gov\\.hmrc\\.customs\\.declarations\\.information\\.upload\\.model\\..*"
    ,"uk\\.gov\\.hmrc\\.customs\\.declarations\\.information\\.views\\..*"
    ,".*(Reverse|AuthService|BuildInfo|Routes|ApiSubscriptionFieldsResponse).*"
  ).mkString(";"),
  coverageMinimumStmtTotal := 96,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  Test / parallelExecution := false
)

PlayKeys.devSettings := Seq("play.server.http.port" -> "9834")

def integrationComponentTestFilter(name: String): Boolean = (name startsWith "it/test/integration") || (name startsWith "it/test/component")
def unitTestFilter(name: String): Boolean = name startsWith "unit"

Compile / unmanagedResourceDirectories += baseDirectory.value / "public"
Test / unmanagedResourceDirectories += baseDirectory.value / "test" / "resources"
(Runtime / managedClasspath) += (Assets / packageBin).value

libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test

// Task to create a ZIP file containing all WCO XSDs for each version, under the version directory
val zipWcoXsds = taskKey[Pipeline.Stage]("Zips up all WCO status XSDs and example messages")

zipWcoXsds := { mappings: Seq[PathMapping] =>
  val targetDir = WebKeys.webTarget.value / "zip"
  val zipFiles: Iterable[java.io.File] =
    ((Assets / resourceDirectory).value / "api" / "conf")
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
