import sbt._

object AppDependencies {

  private val testScope = "test,it"
  private val bootstrapVersion = "7.15.0"

  val compile = Seq(
    "uk.gov.hmrc"            %% "bootstrap-backend-play-28"   % bootstrapVersion,
    "org.typelevel"          %% "cats-core"                   % "2.10.0"
  )

  val test = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play"     % "5.1.0"          % testScope,
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootstrapVersion % testScope,
    "com.github.tomakehurst" %  "wiremock-standalone"    % "2.27.2"         % testScope,
    "org.scalatestplus"      %% "mockito-3-4"            % "3.2.10.0"       % testScope,
    "com.vladsch.flexmark"   %  "flexmark-all"           % "0.35.10"        % testScope
  )
}
