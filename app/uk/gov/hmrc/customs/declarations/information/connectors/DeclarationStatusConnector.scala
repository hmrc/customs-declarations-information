/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.google.inject._
import org.joda.time.DateTime
import play.api.http.HeaderNames._
import play.api.http.{MimeTypes, Status}
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.api.common.connectors.CircuitBreakerConnector
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{AuthorisedRequest, HasConversationId}
import uk.gov.hmrc.customs.declarations.information.services.InformationConfigService
import uk.gov.hmrc.customs.declarations.information.xml.BackendPayloadCreator
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class DeclarationStatusConnector @Inject()(val http: HttpClient,
                                           val logger: InformationLogger,
                                           val backendPayloadCreator: BackendPayloadCreator,
                                           val serviceConfigProvider: ServiceConfigProvider,
                                           val config: InformationConfigService,
                                           val cdsLogger: CdsLogger,
                                           val actorSystem: ActorSystem)
                                          (implicit val ec: ExecutionContext)
  extends CircuitBreakerConnector with HttpErrorFunctions with Status {

  override val configKey = "declaration-status"

  override lazy val numberOfCallsToTriggerStateChange = config.informationCircuitBreakerConfig.numberOfCallsToTriggerStateChange
  override lazy val unstablePeriodDurationInMillis = config.informationCircuitBreakerConfig.unstablePeriodDurationInMillis
  override lazy val unavailablePeriodDurationInMillis = config.informationCircuitBreakerConfig.unavailablePeriodDurationInMillis

  def send[A](date: DateTime,
              correlationId: CorrelationId,
              apiVersion: ApiVersion,
              maybeApiSubscriptionFieldsResponse: Option[ApiSubscriptionFieldsResponse],
              searchType: SearchType)(implicit asr: AuthorisedRequest[A]): Future[HttpResponse] = {

    val config = Option(serviceConfigProvider.getConfig(s"${apiVersion.configPrefix}$configKey")).getOrElse(throw new IllegalArgumentException("config not found"))
    val bearerToken = "Bearer " + config.bearerToken.getOrElse(throw new IllegalStateException("no bearer token was found in config"))
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = getHeaders(date, asr.conversationId, correlationId), authorization = Some(Authorization(bearerToken)))

    val declarationStatusPayload = backendPayloadCreator.create(correlationId, date, searchType, maybeApiSubscriptionFieldsResponse)
    withCircuitBreaker(post(declarationStatusPayload, config.url))
  }

  private def getHeaders(date: DateTime, conversationId: ConversationId, correlationId: CorrelationId) = {
    Seq(
        (X_FORWARDED_HOST, "MDTP"),
        (XCorrelationIdHeaderName, correlationId.toString),
        (XConversationIdHeaderName, conversationId.toString),
        (DATE, date.toString("EEE, dd MMM yyyy HH:mm:ss z")),
        (CONTENT_TYPE, MimeTypes.XML + "; charset=utf-8"),
        (ACCEPT, MimeTypes.XML)
    )
  }

  private def post[A](xml: NodeSeq, url: String)(implicit asr: AuthorisedRequest[A], hc: HeaderCarrier) = {
    logger.debug(s"Sending request to $url. Headers ${hc.headers} Payload: ${xml.toString()}")

    val startTime = LocalDateTime.now
    http.POSTString[HttpResponse](url, xml.toString()).map { response =>
      logCallDuration(startTime)
      logger.debugFull(s"response status: ${response.status} response body: ${response.body}")

      response.status match {
        case status if is2xx(status) =>
          response
        case status => //1xx, 3xx, 4xx, 5xx
          throw new Non2xxResponseException(response, status)
      }
    }.recoverWith {
        case httpError: HttpException =>
          logger.error(s"Call to declaration status failed. url=$url HttpStatus=${httpError.responseCode} error=${httpError.getMessage}")
          Future.failed(httpError)
        case e: Throwable =>
          logger.error(s"Call to declaration status failed. url=$url")
          Future.failed(e)
      }
  }

  override protected def breakOnException(t: Throwable): Boolean = t match {
    case e: Non2xxResponseException => e.responseCode match {
      case BAD_REQUEST => false
      case NOT_FOUND => false
      case _ => true
    }
    case _ => true
  }

  protected def logCallDuration(startTime: LocalDateTime)(implicit r: HasConversationId): Unit ={
    val callDuration = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now)
    logger.info(s"Outbound call duration was $callDuration ms")
  }

}
