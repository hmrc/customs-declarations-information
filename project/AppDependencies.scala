import sbt.*

object AppDependencies {
  private val bootstrapVersion = "9.6.0"
  private val playVersion = "play-30"

  val compile = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "org.typelevel"          %% "cats-core"                       % "2.12.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion" % bootstrapVersion % Test
  )
}
