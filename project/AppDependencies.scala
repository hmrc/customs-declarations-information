import sbt._

object AppDependencies {

  private val scalatestplusVersion = "5.1.0"
  private val mockitoVersion = "3.11.2"
  private val wireMockVersion = "2.29.1"
  private val customsApiCommonVersion = "1.57.0"
  private val testScope = "test,it"

  val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % scalatestplusVersion % testScope

  val wireMock = "com.github.tomakehurst" % "wiremock-jre8" % wireMockVersion % testScope

  val mockito =  "org.mockito" % "mockito-core" % mockitoVersion % testScope

  val customsApiCommon = "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion withSources()

  val customsApiCommonTests = "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion % testScope classifier "tests"

  val flexmark = "com.vladsch.flexmark" % "flexmark-all" % "0.35.10"

  val jacksonModule = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.2"
}
