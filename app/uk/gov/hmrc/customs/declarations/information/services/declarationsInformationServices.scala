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

package uk.gov.hmrc.customs.declarations.information.services

import play.api.http.HttpEntity
import play.api.mvc.Result
import play.mvc.Http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, ErrorPayloadForbidden, errorInternalServerError}
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationConnector._
import uk.gov.hmrc.customs.declarations.information.connectors._
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.SearchType
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{AuthorisedRequest, HasConversationId}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Elem, XML}

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

  protected def matchErrorCode(errorCodeText: String): ErrorResponse = {
    errorCodeText.toLowerCase() match {
      case "cds60001" => backendCDS60001NotFoundResponse
      case "cds60002" => backendCDS60002SearchInvalidResponse
      case "cds60003" => backendCDS60003InternalServerErrorResponse
      case _ => ErrorInternalServerError
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
                                          uniqueIdsService: UniqueIdsService,
                                          config: InformationConfigService)
                                         (implicit override val ec: ExecutionContext)
  extends DeclarationService(apiSubFieldsConnector, logger, connector, dateTimeProvider, uniqueIdsService) {

  protected val endpointName: String = "version"

  protected def matchErrorCode(errorCodeText: String): ErrorResponse = {
    errorCodeText.toLowerCase() match {
      case "cds60001" => backendCDS60001NotFoundResponse
      case "cds60002" => backendCDS60002MrnInvalidResponse
      case "cds60003" => backendCDS60003InternalServerErrorResponse
      case "cds60011" => backendCDS60011SubmissionChannelInvalidResponse
      case _ => ErrorInternalServerError
    }
  }

  protected def filterResponse(response: HttpResponse, xmlResponseBody: Elem): HttpResponse = {
    val responseXml = versionResponseFilterService.transform(xmlResponseBody).head
    HttpResponse(response.status, responseXml.toString(), response.headers)
  }
}

@Singleton
class DeclarationSearchService @Inject()(searchResponseFilterService: SearchResponseFilterService,
                                         override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                         override val logger: InformationLogger,
                                         connector: DeclarationSearchConnector,
                                         dateTimeProvider: DateTimeService,
                                         uniqueIdsService: UniqueIdsService,
                                         config: InformationConfigService)
                                        (implicit override val ec: ExecutionContext)
  extends DeclarationService(apiSubFieldsConnector, logger, connector, dateTimeProvider, uniqueIdsService) {

  protected val endpointName: String = "search"
  protected val backendCDS60005PageOutOfBoundsResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60005", "pageNumber parameter out of bounds")
  protected val backendCDS60006PartyRoleInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60006", "Invalid partyRole parameter")
  protected val backendCDS60007DeclarationStatusInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60007", "Invalid declarationStatus parameter")
  protected val backendCDS60008DeclarationCategoryInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60008", "Invalid declarationCategory parameter")
  protected val backendCDS60009DateInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60009", "Invalid date parameters")
  protected val backendCDS60010GoodsLocationCodeInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60010", "Invalid goodsLocationCode parameter")
  protected val backendCDS60012PageNumberInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60012", "Invalid pageNumber parameter")

  protected def matchErrorCode(errorCodeText: String): ErrorResponse = {
    errorCodeText.toLowerCase() match {
      case "cds60001" => backendCDS60001NotFoundResponse
      case "cds60002" => backendCDS60002MrnInvalidResponse
      case "cds60003" => backendCDS60003InternalServerErrorResponse
      case "cds60005" => backendCDS60005PageOutOfBoundsResponse
      case "cds60006" => backendCDS60006PartyRoleInvalidResponse
      case "cds60007" => backendCDS60007DeclarationStatusInvalidResponse
      case "cds60008" => backendCDS60008DeclarationCategoryInvalidResponse
      case "cds60009" => backendCDS60009DateInvalidResponse
      case "cds60010" => backendCDS60010GoodsLocationCodeInvalidResponse
      case "cds60011" => backendCDS60011SubmissionChannelInvalidResponse
      case "cds60012" => backendCDS60012PageNumberInvalidResponse
      case _ => ErrorInternalServerError
    }
  }

  protected def filterResponse(response: HttpResponse, xmlResponseBody: Elem): HttpResponse = {
    val responseXml = searchResponseFilterService.transform(xmlResponseBody).head
    HttpResponse(response.status, responseXml.toString(), response.headers)
  }
}

@Singleton
class DeclarationFullService @Inject()(fullResponseFilterService: FullResponseFilterService,
                                       override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                       override val logger: InformationLogger,
                                       connector: DeclarationFullConnector,
                                       dateTimeProvider: DateTimeService,
                                       uniqueIdsService: UniqueIdsService,
                                       config: InformationConfigService)
                                      (implicit override val ec: ExecutionContext)
  extends DeclarationService(apiSubFieldsConnector, logger, connector, dateTimeProvider, uniqueIdsService) {

  protected val endpointName: String = "declaration-full"

  protected def matchErrorCode(errorCodeText: String): ErrorResponse = {
    errorCodeText.toLowerCase() match {
      case "cds60001" => backendCDS60001NotFoundResponse
      case "cds60002" => backendCDS60002MrnInvalidResponse
      case "cds60003" => backendCDS60003InternalServerErrorResponse
      case "cds60011" => backendCDS60011SubmissionChannelInvalidResponse
      case _ => ErrorInternalServerError
    }
  }

  protected def filterResponse(response: HttpResponse, xmlResponseBody: Elem): HttpResponse = {
    val responseXml = fullResponseFilterService.transform(xmlResponseBody).head
    HttpResponse(response.status, responseXml.toString(), response.headers)
  }
}

abstract class DeclarationService @Inject()(override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                            override val logger: InformationLogger,
                                            connector: DeclarationConnector,
                                            dateTimeProvider: DateTimeService,
                                            uniqueIdsService: UniqueIdsService)
                                           (implicit val ec: ExecutionContext) extends ApiSubscriptionFieldsService {

  protected def matchErrorCode(errorCodeText: String): ErrorResponse

  protected def filterResponse(response: HttpResponse, xmlResponseBody: Elem): HttpResponse

  protected val endpointName: String

  protected val errorResponseServiceUnavailable: ErrorResponse = errorInternalServerError("This service is currently unavailable")
  protected val backendCDS60001NotFoundResponse: ErrorResponse = ErrorResponse(NOT_FOUND, "CDS60001", "Declaration not found")
  protected val backendCDS60002MrnInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60002", "MRN parameter invalid")
  protected val backendCDS60002SearchInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60002", "Search parameter invalid")
  protected val backendCDS60003InternalServerErrorResponse: ErrorResponse = ErrorResponse(INTERNAL_SERVER_ERROR, "CDS60003", ErrorInternalServerError.message)
  protected val backendCDS60011SubmissionChannelInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60011", "Invalid declarationSubmissionChannel parameter")

  def send[A](searchType: SearchType)(implicit asr: AuthorisedRequest[A], hc: HeaderCarrier): Future[Either[Result, HttpResponse]] = {

    val dateTime = dateTimeProvider.nowUtc()
    val correlationId = uniqueIdsService.correlation

    futureApiSubFieldsId(asr.clientId).flatMap {
      case Left(result) =>
        Future.successful(Left(result))
      case Right(sfId) =>
        connector.send(dateTime, correlationId, asr.requestedApiVersion, sfId, searchType)
          .map {
            case Right(response) =>
              logger.warn(s"Response is ${response.body}")
              val filteredResponse = filterResponse(response, XML.loadString(response.body))
              Right(filteredResponse)
            case Left(RetryError) =>
              handleError("Unhealthy state entered", INTERNAL_SERVER_ERROR, errorResponseServiceUnavailable)
            case Left(UnexpectedError(t)) =>
              handleError(t.getMessage, INTERNAL_SERVER_ERROR, ErrorInternalServerError)
            case Left(Non2xxResponseError(status, body)) =>
              status match {
                case INTERNAL_SERVER_ERROR =>
                  val errorCodeText = (XML.loadString(body) \ "errorCode").text
                  val errorResponse: ErrorResponse = matchErrorCode(errorCodeText)
                  logger.warn(s"declaration [$endpointName] call failed with backend http status code of [500] and error: [${errorResponse.errorCode}] so returning to consumer " +
                    s"[${errorResponse.httpStatusCode}] and response body: [${errorResponse.XmlResult.body.asInstanceOf[HttpEntity.Strict].data.utf8String}]")
                  Left(errorResponse.XmlResult.withConversationId)
                case FORBIDDEN =>
                  logger.warn(s"declaration [$endpointName] call failed with backend http status code of [403] so returning to consumer [403]")
                  Left(ErrorPayloadForbidden.XmlResult.withConversationId)
                case unexpectedStatus =>
                  handleError(s"unexpected backend http status code of [$unexpectedStatus]", INTERNAL_SERVER_ERROR, ErrorInternalServerError)
              }
          }
    }
  }

  private def handleError(message: String, statusToReturn: Int, errorResponse: ErrorResponse)(implicit r: HasConversationId) = {
    logger.error(s"declaration[$endpointName] call failed: $message, returning status code [$statusToReturn]")
    Left(errorResponse.XmlResult.withConversationId)
  }
}
