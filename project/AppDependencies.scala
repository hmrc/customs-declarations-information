import sbt.*

object AppDependencies {
  private val bootstrapVersion = "9.13.0"
  private val playVersion = "play-30"

  val compile = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "org.typelevel"          %% "cats-core"                       % "2.13.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion" % bootstrapVersion % Test,
    "org.wiremock"           % "wiremock-standalone"             % "3.10.0"     % Test
  )
}
