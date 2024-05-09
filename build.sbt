import com.typesafe.sbt.web.PathMapping
import com.typesafe.sbt.web.pipeline.Pipeline
import sbt.Keys.*
import sbt.Tests.{Group, SubProcess}
import sbt.*
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, targetJvm}
import uk.gov.hmrc.gitstamp.GitStampPlugin.*
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import play.sbt.PlayImport.PlayKeys.playDefaultPort
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.language.postfixOps

name := "customs-declarations-information"
scalaVersion := "2.13.13"
targetJvm := "jvm-11"
//Test / fork := false

lazy val ComponentTest = config("component") extend Test
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
    integrationComponentTestSettings,
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

lazy val integrationComponentTestSettings =
  inConfig(CdsIntegrationComponentTest)(Defaults.testTasks) ++
    Seq(
      CdsIntegrationComponentTest / testOptions := Seq(Tests.Filter(integrationComponentTestFilter)),
      CdsIntegrationComponentTest / parallelExecution := false,
      addTestReportOption(CdsIntegrationComponentTest, "int-comp-test-reports"),
      CdsIntegrationComponentTest / testGrouping := forkedJvmPerTestConfig((Test / definedTests).value, "integration", "component")
    )

lazy val commonSettings: Seq[Setting[_]] = gitStampSettings

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
  Test / parallelExecution := false
)

PlayKeys.devSettings := Seq("play.server.http.port" -> "9834")

def integrationComponentTestFilter(name: String): Boolean = (name startsWith "integration") || (name startsWith "component")
def unitTestFilter(name: String): Boolean = name startsWith "unit"

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

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
