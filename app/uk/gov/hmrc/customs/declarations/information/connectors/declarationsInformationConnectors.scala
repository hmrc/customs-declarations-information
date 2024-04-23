/*
 * Copyright 2024 HM Revenue & Customs
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

import com.google.inject._
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.CircuitBreakerOpenException
import play.api.http.HeaderNames._
import play.api.http.{MimeTypes, Status}
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.api.common.connectors.CircuitBreakerConnector
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationConnector._
import uk.gov.hmrc.customs.declarations.information.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.customs.declarations.information.services.InformationConfigService
import uk.gov.hmrc.customs.declarations.information.xml._
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http._

import java.time.{LocalDateTime, ZonedDateTime}
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeclarationStatusConnector @Inject()(http: HttpClient,
                                           logger: InformationLogger,
                                           backendPayloadCreator: BackendStatusPayloadCreator,
                                           serviceConfigProvider: ServiceConfigProvider,
                                           config: InformationConfigService,
                                           override val cdsLogger: CdsLogger,
                                           override val actorSystem: ActorSystem)
                                          (implicit override val ec: ExecutionContext)
  extends DeclarationConnector(http, logger, backendPayloadCreator, serviceConfigProvider, config) {

  override val configKey = "declaration-status"

  override lazy val numberOfCallsToTriggerStateChange = config.informationCircuitBreakerConfig.numberOfCallsToTriggerStateChange
  override lazy val unstablePeriodDurationInMillis = config.informationCircuitBreakerConfig.unstablePeriodDurationInMillis
  override lazy val unavailablePeriodDurationInMillis = config.informationCircuitBreakerConfig.unavailablePeriodDurationInMillis
}

@Singleton
class DeclarationVersionConnector @Inject()(http: HttpClient,
                                            logger: InformationLogger,
                                            backendPayloadCreator: BackendVersionPayloadCreator,
                                            serviceConfigProvider: ServiceConfigProvider,
                                            config: InformationConfigService,
                                            override val cdsLogger: CdsLogger,
                                            override val actorSystem: ActorSystem)
                                           (implicit override val ec: ExecutionContext)
  extends DeclarationConnector(http, logger, backendPayloadCreator, serviceConfigProvider, config) {

  override val configKey = "declaration-version"

  override lazy val numberOfCallsToTriggerStateChange: Int = config.informationCircuitBreakerConfig.numberOfCallsToTriggerStateChange
  override lazy val unstablePeriodDurationInMillis: Int = config.informationCircuitBreakerConfig.unstablePeriodDurationInMillis
  override lazy val unavailablePeriodDurationInMillis: Int = config.informationCircuitBreakerConfig.unavailablePeriodDurationInMillis
}

@Singleton
class DeclarationSearchConnector @Inject()(http: HttpClient,
                                           logger: InformationLogger,
                                           backendPayloadCreator: BackendSearchPayloadCreator,
                                           serviceConfigProvider: ServiceConfigProvider,
                                           config: InformationConfigService,
                                           override val cdsLogger: CdsLogger,
                                           override val actorSystem: ActorSystem)(implicit override val ec: ExecutionContext)
  extends DeclarationConnector(http, logger, backendPayloadCreator, serviceConfigProvider, config) {

  override val configKey = "declaration-search"

  override lazy val numberOfCallsToTriggerStateChange = config.informationCircuitBreakerConfig.numberOfCallsToTriggerStateChange
  override lazy val unstablePeriodDurationInMillis = config.informationCircuitBreakerConfig.unstablePeriodDurationInMillis
  override lazy val unavailablePeriodDurationInMillis = config.informationCircuitBreakerConfig.unavailablePeriodDurationInMillis
}

@Singleton
class DeclarationFullConnector @Inject()(http: HttpClient,
                                         logger: InformationLogger,
                                         backendPayloadCreator: BackendFullPayloadCreator,
                                         serviceConfigProvider: ServiceConfigProvider,
                                         config: InformationConfigService,
                                         override val cdsLogger: CdsLogger,
                                         override val actorSystem: ActorSystem)
                                        (implicit override val ec: ExecutionContext)
  extends DeclarationConnector(http, logger, backendPayloadCreator, serviceConfigProvider, config) {

  override val configKey = "declaration-full"

  override lazy val numberOfCallsToTriggerStateChange = config.informationCircuitBreakerConfig.numberOfCallsToTriggerStateChange
  override lazy val unstablePeriodDurationInMillis = config.informationCircuitBreakerConfig.unstablePeriodDurationInMillis
  override lazy val unavailablePeriodDurationInMillis = config.informationCircuitBreakerConfig.unavailablePeriodDurationInMillis
}

abstract class DeclarationConnector @Inject()(http: HttpClient,
                                              logger: InformationLogger,
                                              backendPayloadCreator: BackendPayloadCreator,
                                              serviceConfigProvider: ServiceConfigProvider,
                                              config: InformationConfigService)
                                             (implicit val ec: ExecutionContext)
  extends CircuitBreakerConnector with Status {

  override lazy val numberOfCallsToTriggerStateChange = config.informationCircuitBreakerConfig.numberOfCallsToTriggerStateChange
  override lazy val unstablePeriodDurationInMillis = config.informationCircuitBreakerConfig.unstablePeriodDurationInMillis
  override lazy val unavailablePeriodDurationInMillis = config.informationCircuitBreakerConfig.unavailablePeriodDurationInMillis

  def send[A](date: ZonedDateTime,
              correlationId: CorrelationId,
              apiVersion: ApiVersion,
              maybeApiSubscriptionFieldsResponse: Option[ApiSubscriptionFieldsResponse],
              searchType: SearchType)(implicit asr: AuthorisedRequest[A]): Future[Either[DeclarationConnector.Error, HttpResponse]] = {
    val x = ZonedDateTime.now()

    val config = Option(serviceConfigProvider.getConfig(s"${apiVersion.configPrefix}$configKey")).getOrElse(throw new IllegalArgumentException("config not found"))
    val url = config.url
    val bearerToken = "Bearer " + config.bearerToken.getOrElse(throw new IllegalStateException("no bearer token was found in config"))
    val headers: Seq[(String, String)] = getHeaders(x, asr.conversationId, correlationId) ++ Seq((AUTHORIZATION, bearerToken))

    val declarationPayload = backendPayloadCreator.create(asr.conversationId, correlationId, x, searchType, maybeApiSubscriptionFieldsResponse)

    case class Non2xxResponseException(status: Int, responseBody: String) extends Throwable
    withCircuitBreaker {
      logger.debug(s"Sending request to [$url]. Headers: [$headers] Payload: [${declarationPayload.toString()}]")
      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
      http.POSTString(url, declarationPayload.toString(), headers).map { response =>
        logger.debugFull(s"response status: [${response.status}] response body: [${response.body}]")

        response.status match {
          case status if Status.isSuccessful(status) =>
            Right(response)
          case status => // Refactor out usage of exceptions 'eventually', but for now maintaining breakOnException() triggering behaviour
            throw Non2xxResponseException(status, response.body)
        }
      }
    }.recover {
      case _: CircuitBreakerOpenException =>
        Left(RetryError)
      case Non2xxResponseException(status, responseBody) =>
        Left(Non2xxResponseError(status, responseBody))
      case t: Throwable =>
        Left(UnexpectedError(t))
    }
  }

  private def getHeaders(date: ZonedDateTime, conversationId: ConversationId, correlationId: CorrelationId): Seq[(String, String)] = {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z")

    Seq(
      (X_FORWARDED_HOST, "MDTP"),
      (XCorrelationIdHeaderName, correlationId.toString),
      (XConversationIdHeaderName, conversationId.toString),
      (DATE, date.format(dateTimeFormatter)),
      (CONTENT_TYPE, MimeTypes.XML + "; charset=utf-8"),
      (ACCEPT, MimeTypes.XML)
    )
  }
}

object DeclarationConnector {
  sealed trait Error

  case class Non2xxResponseError(status: Int, responseBody: String) extends Error

  case object RetryError extends Error

  case class UnexpectedError(t: Throwable) extends Error
}
