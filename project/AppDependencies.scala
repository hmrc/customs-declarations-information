import sbt.*

object AppDependencies {
  private val bootstrapVersion = "9.11.0"
  private val playVersion = "play-30"

  val compile = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "org.typelevel"          %% "cats-core"                       % "2.12.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion" % bootstrapVersion % Test,
    "org.mockito"                   %% "mockito-scala-scalatest"         % "1.17.37"    % Test,
    "org.wiremock"                   % "wiremock-standalone"             % "3.10.0"     % Test
  )
}
