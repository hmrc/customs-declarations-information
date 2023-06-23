import sbt._

object AppDependencies {

  private val testScope = "test,it"
  private val customsApiCommonVersion = "1.60.0"

  val scalaTestPlusPlay     = "org.scalatestplus.play"       %% "scalatestplus-play"     % "5.1.0"   % testScope
  val wireMock              = "com.github.tomakehurst"       %  "wiremock-standalone"    % "2.27.2"   % testScope
  val mockito               = "org.scalatestplus"            %% "mockito-3-4"             % "3.2.10.0" % testScope
  val customsApiCommon      = "uk.gov.hmrc"                  %% "customs-api-common"     % customsApiCommonVersion  withSources()
  val customsApiCommonTests = "uk.gov.hmrc"                  %% "customs-api-common"     % customsApiCommonVersion  % testScope classifier "tests"
  val flexmark              = "com.vladsch.flexmark"         %  "flexmark-all"           % "0.35.10" % testScope
}
