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

import com.google.inject.Inject
import org.apache.pekko.pattern.CircuitBreakerOpenException
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION, CONTENT_TYPE, DATE, X_FORWARDED_HOST}
import play.api.http.{MimeTypes, Status}
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.api.common.connectors.CircuitBreakerConnector
import uk.gov.hmrc.customs.declarations.information.config.ConfigService
import uk.gov.hmrc.customs.declarations.information.util.CustomHeaderNames.{XConversationIdHeaderName, XCorrelationIdHeaderName}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.{ApiSubscriptionFieldsResponse, ApiVersion, AuthorisedRequest, ConversationId, CorrelationId, SearchType}
import uk.gov.hmrc.customs.declarations.information.xml.BackendPayloadCreator
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq
import uk.gov.hmrc.customs.declarations.information.connectors.AbstractDeclarationConnector._
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

abstract class AbstractDeclarationConnector @Inject()(http: HttpClient,
                                                      logger: InformationLogger,
                                                      backendPayloadCreator: BackendPayloadCreator,
                                                      serviceConfigProvider: ServiceConfigProvider,
                                                      config: ConfigService)(implicit val ec: ExecutionContext) extends CircuitBreakerConnector with Status {
  override lazy val numberOfCallsToTriggerStateChange: Int = config.informationCircuitBreakerConfig.numberOfCallsToTriggerStateChange
  override lazy val unstablePeriodDurationInMillis: Int = config.informationCircuitBreakerConfig.unstablePeriodDurationInMillis
  override lazy val unavailablePeriodDurationInMillis: Int = config.informationCircuitBreakerConfig.unavailablePeriodDurationInMillis

  def send[A](date: ZonedDateTime,
              correlationId: CorrelationId,
              apiVersion: ApiVersion,
              maybeApiSubscriptionFieldsResponse: Option[ApiSubscriptionFieldsResponse],
              searchType: SearchType)(implicit asr: AuthorisedRequest[A]): Future[Either[AbstractDeclarationConnector.Error, HttpResponse]] = {
    val config: ServiceConfig = Option(serviceConfigProvider.getConfig(s"${apiVersion.configPrefix}$configKey")).getOrElse(throw new IllegalArgumentException("config not found"))
    val url: String = config.url
    val bearerToken: String = "Bearer " + config.bearerToken.getOrElse(throw new IllegalStateException("no bearer token was found in config"))
    val headers: Seq[(String, String)] = getHeaders(date, asr.conversationId, correlationId) ++ Seq((AUTHORIZATION, bearerToken))
    val declarationPayload: NodeSeq = backendPayloadCreator.create(
      asr.conversationId,
      correlationId,
      date,
      searchType,
      maybeApiSubscriptionFieldsResponse)

    //TODO remove/move this
    case class Non2xxResponseException(status: Int, responseBody: String) extends Throwable

    withCircuitBreaker {
      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
      logger.debug(s"Sending request to [$url]. Headers: [$headers] Payload: [${declarationPayload.toString()}]")

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
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z")

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

//TODO hmmm?
object AbstractDeclarationConnector {
  sealed trait Error

  case class Non2xxResponseError(status: Int, responseBody: String) extends Error

  case object RetryError extends Error

  case class UnexpectedError(t: Throwable) extends Error
}
