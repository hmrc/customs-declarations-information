import sbt.*

object AppDependencies {
  private val bootstrapVersion = "8.5.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-backend-play-30"   % bootstrapVersion,
    "org.typelevel"          %% "cats-core"                   % "2.10.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play"     % "7.0.1"          % Test,
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "com.github.tomakehurst" %  "wiremock-standalone"    % "3.0.1"          % Test,
    "org.scalatestplus"      %% "mockito-3-4"            % "3.2.10.0"       % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"           % "0.64.8"         % Test
  )
}
