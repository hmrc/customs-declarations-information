import sbt._

object AppDependencies {

  private val testScope = "test,it"
  private val customsApiCommonVersion = "1.60.0"

  val bootstrapBackendPlay28  = "uk.gov.hmrc"                  %% "bootstrap-backend-play-28"   % "7.15.0"
  val cats                    = "org.typelevel"                %% "cats-core"                   % "2.10.0"
  val scalaTestPlusPlay       = "org.scalatestplus.play"       %% "scalatestplus-play"          % "5.1.0"     % testScope
  val wireMock                = "com.github.tomakehurst"       %  "wiremock-standalone"         % "2.27.2"    % testScope
  val mockito                 = "org.scalatestplus"            %% "mockito-3-4"                 % "3.2.10.0"  % testScope
  val flexmark                = "com.vladsch.flexmark"         %  "flexmark-all"                % "0.35.10"   % testScope
}
