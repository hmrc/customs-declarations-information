import sbt._

object AppDependencies {

  private val testScope = "test,it"

  val scalaTestPlusPlay     = "org.scalatestplus.play"       %% "scalatestplus-play"     % "5.1.0"   % testScope
  val wireMock              = "com.github.tomakehurst"       %  "wiremock-jre8"          % "2.34.0"  % testScope
  val mockito               = "org.mockito"                  %  "mockito-core"           % "4.8.1"   % testScope
  val customsApiCommon      = "uk.gov.hmrc"                  %% "customs-api-common"     % "1.57.0"  withSources()
  val customsApiCommonTests = "uk.gov.hmrc"                  %% "customs-api-common"     % "1.57.0"  % testScope classifier "tests"
  val flexmark              = "com.vladsch.flexmark"         %  "flexmark-all"           % "0.35.10" % testScope
  val jacksonModule         = "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.13.4"
  val bootstrapTestPlay     = "uk.gov.hmrc"                  %% "bootstrap-test-play-28" % "7.13.0"  % testScope
}
