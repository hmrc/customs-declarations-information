import sbt.*

object AppDependencies {
  private val bootstrapVersion = "8.5.0"

  val compile = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel"          %% "cats-core"                  % "2.10.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootstrapVersion % Test
  )
}
