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

package uk.gov.hmrc.customs.declarations.information.services

import akka.pattern.CircuitBreakerOpenException
import play.api.http.HttpEntity
import play.api.mvc.Result
import play.mvc.Http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, NotFoundCode, errorInternalServerError}
import uk.gov.hmrc.customs.declarations.information.connectors.{ApiSubscriptionFieldsConnector, DeclarationConnector, DeclarationStatusConnector, DeclarationVersionConnector, Non2xxResponseException}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.{Mrn, SearchType}
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{AuthorisedRequest, HasConversationId}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse}

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left
import scala.util.control.NonFatal
import scala.xml.{Elem, NodeSeq, XML}

@Singleton
class DeclarationStatusService @Inject()(statusResponseFilterService: StatusResponseFilterService,
                                         override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                         override val logger: InformationLogger,
                                         connector: DeclarationStatusConnector,
                                         dateTimeProvider: DateTimeService,
                                         uniqueIdsService: UniqueIdsService)
                                        (implicit override val ec: ExecutionContext)
  extends DeclarationService(apiSubFieldsConnector, logger, connector, dateTimeProvider, uniqueIdsService) {

  protected val endpointName: String = "status"

  protected def matchErrorCode[A](errorCodeText: String)(implicit asr: AuthorisedRequest[A], hc: HeaderCarrier): Either[Result, HttpResponse] = {
    errorCodeText.toLowerCase() match {
      case "cds60001" => processError(backendCDS60001NotFoundResponse)
      case "cds60002" => processError(backendCDS60002SearchInvalidResponse)
      case "cds60003" => processError(backendCDS60003InternalServerErrorResponse)
      case _ => processError(backendCDS60003InternalServerErrorResponse)
    }
  }

  def filterResponse(response: HttpResponse, xmlResponseBody: Elem): HttpResponse = {
    val responseXml = statusResponseFilterService.transform(xmlResponseBody).head
    HttpResponse(response.status, responseXml.toString(), response.headers)
  }

}

@Singleton
class DeclarationVersionService @Inject()(versionResponseFilterService: VersionResponseFilterService,
                                          override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                          override val logger: InformationLogger,
                                          connector: DeclarationVersionConnector,
                                          dateTimeProvider: DateTimeService,
                                          uniqueIdsService: UniqueIdsService)
                                         (implicit override val ec: ExecutionContext)
  extends DeclarationService(apiSubFieldsConnector, logger, connector, dateTimeProvider, uniqueIdsService) {

  protected val endpointName: String = "version"

  protected def matchErrorCode[A](errorCodeText: String)(implicit asr: AuthorisedRequest[A], hc: HeaderCarrier): Either[Result, HttpResponse] = {
    errorCodeText.toLowerCase() match {
      case "cds60001" => processError(backendCDS60001NotFoundResponse)
      case "cds60002" => processError(backendCDS60002MrnInvalidResponse)
      case "cds60003" => processError(backendCDS60003InternalServerErrorResponse)
      case "cds60011" => processError(backendCDS60011InvalidSubmissionChannelResponse)
      case _ => processError(backendCDS60003InternalServerErrorResponse)
    }
  }

  protected def filterResponse(response: HttpResponse, xmlResponseBody: Elem): HttpResponse = {
    val responseXml = versionResponseFilterService.transform(xmlResponseBody).head
    HttpResponse(response.status, responseXml.toString(), response.headers)
  }
}

abstract class DeclarationService @Inject()(override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                            override val logger: InformationLogger,
                                            connector: DeclarationConnector,
                                            dateTimeProvider: DateTimeService,
                                            uniqueIdsService: UniqueIdsService)
                                           (implicit val ec: ExecutionContext) extends ApiSubscriptionFieldsService {

  protected def matchErrorCode[A](errorCodeText: String)(implicit asr: AuthorisedRequest[A], hc: HeaderCarrier): Either[Result, HttpResponse]
  protected def filterResponse(response: HttpResponse, xmlResponseBody: Elem): HttpResponse
  protected val endpointName: String

  protected val errorResponseServiceUnavailable: ErrorResponse = errorInternalServerError("This service is currently unavailable")
  protected val customNotFoundResponse: ErrorResponse = ErrorResponse(NOT_FOUND, NotFoundCode, "Declaration not found")
  protected val backendCDS60001NotFoundResponse: ErrorResponse = ErrorResponse(NOT_FOUND, "CDS60001", "Declaration not found")
  protected val backendCDS60002MrnInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60002", "MRN parameter invalid")
  protected val backendCDS60002SearchInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60002", "Search parameter invalid")
  protected val backendCDS60003InternalServerErrorResponse: ErrorResponse = ErrorResponse(INTERNAL_SERVER_ERROR, "CDS60003", ErrorInternalServerError.message)
  protected val backendCDS60011InvalidSubmissionChannelResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60011", "Invalid declarationSubmissionChannel parameter")

  def send[A](eitherMrnOrSearchType: Either[SearchType, Mrn])(implicit asr: AuthorisedRequest[A], hc: HeaderCarrier): Future[Either[Result, HttpResponse]] = {

    val dateTime = dateTimeProvider.nowUtc()
    val correlationId = uniqueIdsService.correlation

    futureApiSubFieldsId(asr.clientId) flatMap {
      case Right(sfId) =>
        connector.send(dateTime, correlationId, asr.requestedApiVersion, sfId, eitherMrnOrSearchType)
          .map(response => {
            val startTime = LocalDateTime.now
            val filteredResponse = filterResponse(response, XML.loadString(response.body))
            logFilteringDuration(startTime)
            Right(filteredResponse)
          }).recover{
          case e: Non2xxResponseException if e.responseCode == INTERNAL_SERVER_ERROR =>
            val body = XML.loadString(e.response.body)
            val errorCode: NodeSeq = body \ "errorCode"
            val errorCodeText = errorCode.text
            matchErrorCode(errorCodeText)
          case e: Non2xxResponseException if e.responseCode == FORBIDDEN =>
            logger.warn(s"declaration $endpointName call failed with backend http status code of 403: ${e.getMessage} so returning 500 to consumer")
            Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
          case e: HttpException if e.responseCode == NOT_FOUND =>
            logger.warn(s"declaration $endpointName call failed with backend http status code of 404: ${e.getMessage} so returning 500 to consumer")
            Left(customNotFoundResponse.XmlResult.withConversationId)
          case e: HttpException =>
            logger.warn(s"declaration $endpointName call failed with backend http status code of ${e.responseCode}: ${e.getMessage} so returning 500 to consumer")
            Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
          case _: CircuitBreakerOpenException =>
            logger.error("unhealthy state entered so returning 500 to consumer with message service unavailble")
            Left(errorResponseServiceUnavailable.XmlResult.withConversationId)
          case NonFatal(e) =>
            logger.error(s"declaration $endpointName call failed: ${e.getMessage} so returning 500 to consumer", e)
            Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
        }
      case Left(result) =>
        Future.successful(Left(result))
    }
  }

  def processError(errorResponse: ErrorResponse)(implicit r: HasConversationId): Left[Result, Nothing] = {
    logFailureOutcome(errorResponse)
    Left(errorResponse.XmlResult.withConversationId)
  }

  def logFailureOutcome(errorResponse: ErrorResponse)(implicit r: HasConversationId): Unit = {
    logger.warn(s"declaration $endpointName call failed with backend http status code of 500 and error: ${errorResponse.errorCode} so returning to consumer response status ${errorResponse.httpStatusCode} and response body: ${errorResponse.XmlResult.body.asInstanceOf[HttpEntity.Strict].data.utf8String}")
  }

  private def logFilteringDuration(startTime: LocalDateTime)(implicit r: HasConversationId): Unit ={
    val duration = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now)
    logger.info(s"Xml declaration filtering was $duration ms")
  }
}
