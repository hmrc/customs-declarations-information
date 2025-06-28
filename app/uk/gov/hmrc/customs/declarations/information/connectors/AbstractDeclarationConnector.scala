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
import play.api.http.HeaderNames.*
import play.api.http.{MimeTypes, Status}
import play.api.libs.ws.writeableOf_String
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.api.common.connectors.CircuitBreakerConnector
import uk.gov.hmrc.customs.declarations.information.config.ConfigService
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.*
import uk.gov.hmrc.customs.declarations.information.util.CustomHeaderNames.{XConversationIdHeaderName, XCorrelationIdHeaderName}
import uk.gov.hmrc.customs.declarations.information.util.HeaderUtil
import uk.gov.hmrc.customs.declarations.information.xml.BackendPayloadCreator
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq
//TODO i would question the need for this as most of it can probably be done by a service
abstract class AbstractDeclarationConnector @Inject()(http: HttpClientV2,
                                                      logger: InformationLogger,
                                                      backendPayloadCreator: BackendPayloadCreator,
                                                      serviceConfigProvider: ServiceConfigProvider,
                                                      config: ConfigService)(implicit val ec: ExecutionContext)
  extends CircuitBreakerConnector with Status with HeaderUtil  {

  override lazy val numberOfCallsToTriggerStateChange: Int = config.informationCircuitBreakerConfig.numberOfCallsToTriggerStateChange
  override lazy val unstablePeriodDurationInMillis: Int = config.informationCircuitBreakerConfig.unstablePeriodDurationInMillis
  override lazy val unavailablePeriodDurationInMillis: Int = config.informationCircuitBreakerConfig.unavailablePeriodDurationInMillis

  def send[A](date: ZonedDateTime,
              correlationId: CorrelationId,
              apiVersion: ApiVersion,
              maybeApiSubscriptionFieldsResponse: Option[ApiSubscriptionFieldsResponse],
              searchType: SearchType)(implicit asr: AuthorisedRequest[A], hc: HeaderCarrier): Future[Either[ConnectionError, HttpResponse]] = {
    val config: ServiceConfig = Option(serviceConfigProvider.getConfig(s"${apiVersion.configPrefix}$configKey")).getOrElse(throw new IllegalArgumentException("config not found"))
    val url: String = config.url
    val bearerToken: String = "Bearer " + config.bearerToken.getOrElse(throw new IllegalStateException("no bearer token was found in config"))
    val headers: Seq[(String, String)] = getHeaders(date, asr.conversationId, correlationId) ++ Seq((AUTHORIZATION, bearerToken)) ++ getCustomsApiStubExtraHeaders
    val declarationPayload: NodeSeq = backendPayloadCreator.create(
      asr.conversationId,
      correlationId,
      date,
      searchType,
      maybeApiSubscriptionFieldsResponse)

    withCircuitBreaker {
      logger.debug(s"Sending request to [$url]. Headers: [$headers] Payload: [${declarationPayload.toString()}]")

      http
        .post(url"$url")
        .setHeader(headers: _*)
        .withBody(declarationPayload.toString)
        .execute[HttpResponse]
        .map { response =>
          logger.debugFull(s"response status: [${response.status}] response body: [${response.body}]")

          response.status match {
            case status if Status.isSuccessful(status) =>
              Right(response)
            case status => //TODO (comment was already here) Refactor out usage of exceptions 'eventually', but for now maintaining breakOnException() triggering behaviour
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
