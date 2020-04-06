package uk.gov.hmrc.customs.declarations.information.connectors
import java.util.concurrent.TimeUnit.MILLISECONDS

import akka.actor.ActorSystem
import akka.pattern.CircuitBreaker
import com.google.inject._
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.services.InformationConfigService

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

abstract class CircuitBreakerConnector @Inject() (config: InformationConfigService, logger: CdsLogger, sys: ActorSystem)(implicit ec: ExecutionContext) {
  protected val configKey: String

  protected def breakOnException(t: Throwable): Boolean

  protected def withCircuitBreaker[T](body: => Future[T]): Future[T] =
    breaker.withCircuitBreaker(body, defineFailure)

  logger.info(s"Circuit Breaker [$configKey] instance created with config $config")
  lazy val breaker = new CircuitBreaker(
    scheduler = sys.scheduler,
    maxFailures = config.informationCircuitBreakerConfig.numberOfCallsToTriggerStateChange,
    callTimeout = Duration(config.informationCircuitBreakerConfig.unavailablePeriodDurationInMillis, MILLISECONDS),
    resetTimeout = Duration(config.informationCircuitBreakerConfig.unstablePeriodDurationInMillis, MILLISECONDS))
      .onOpen(notifyOnStateChange("Open"))
      .onClose(notifyOnStateChange("Close"))
      .onHalfOpen(notifyOnStateChange("HalfOpen"))

  private def notifyOnStateChange(newState: String): Unit =
    logger.warn(s"circuitbreaker: Service [$configKey] is in state [${newState}]")

  private def defineFailure(t: Try[_]): Boolean = {
    !t.isFailure || breakOnException(t.failed.get)
  }
}
