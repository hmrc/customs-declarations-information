import sbt.*

object AppDependencies {
  private val bootstrapVersion = "8.5.0"
  private val playVersion = "play-30"

  val compile = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "org.typelevel"          %% "cats-core"                       % "2.10.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-$playVersion" % bootstrapVersion % Test
  )
}
