/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

abstract class CircuitBreakerConnector @Inject() (config: InformationConfigService, logger: CdsLogger, actorSystem: ActorSystem)(implicit ec: ExecutionContext) {
  protected val configKey: String

  protected def breakOnException(t: Throwable): Boolean

  protected def withCircuitBreaker[T](body: => Future[T]): Future[T] =
    breaker.withCircuitBreaker(body, defineFailure)

  logger.info(s"Circuit Breaker [$configKey] instance created with config $config")
  lazy val breaker = new CircuitBreaker(
    scheduler = actorSystem.scheduler,
    maxFailures = config.informationCircuitBreakerConfig.numberOfCallsToTriggerStateChange,
    callTimeout = Duration(config.informationCircuitBreakerConfig.unavailablePeriodDurationInMillis, MILLISECONDS),
    resetTimeout = Duration(config.informationCircuitBreakerConfig.unstablePeriodDurationInMillis, MILLISECONDS))
      .onOpen(notifyOnStateChange("Open"))
      .onClose(notifyOnStateChange("Close"))
      .onHalfOpen(notifyOnStateChange("HalfOpen"))

  private def notifyOnStateChange(newState: String): Unit =
    logger.warn(s"circuitbreaker: Service [$configKey] is in state [${newState}]")

  private def defineFailure(t: Try[_]): Boolean =
    t.isFailure && breakOnException(t.failed.get)
}
