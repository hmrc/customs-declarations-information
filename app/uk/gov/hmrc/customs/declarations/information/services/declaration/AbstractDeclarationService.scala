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

package uk.gov.hmrc.customs.declarations.information.services.declaration

import play.api.http.HttpEntity
import play.api.mvc.Result
import play.mvc.Http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, ErrorPayloadForbidden, errorInternalServerError}
import uk.gov.hmrc.customs.declarations.information.connectors.{AbstractDeclarationConnector, ApiSubscriptionFieldsConnector}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.{AuthorisedRequest, CorrelationId, HasConversationId, Non2xxResponseError, RetryError, SearchType, UnexpectedError}
import uk.gov.hmrc.customs.declarations.information.services.{ApiSubscriptionFieldsService, UniqueIdsService}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.time.{Clock, ZoneId, ZonedDateTime}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Elem, XML}

abstract class AbstractDeclarationService @Inject()(override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                                    override val logger: InformationLogger,
                                                    connector: AbstractDeclarationConnector,
                                                    uniqueIdsService: UniqueIdsService)(implicit val ec: ExecutionContext) extends ApiSubscriptionFieldsService {
  protected val endpointName: String
  protected val errorResponseServiceUnavailable: ErrorResponse = errorInternalServerError("This service is currently unavailable")
  protected val backendCDS60001NotFoundResponse: ErrorResponse = ErrorResponse(NOT_FOUND, "CDS60001", "Declaration not found")
  protected val backendCDS60002MrnInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60002", "MRN parameter invalid")
  protected val backendCDS60002SearchInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60002", "Search parameter invalid")
  protected val backendCDS60003InternalServerErrorResponse: ErrorResponse = ErrorResponse(INTERNAL_SERVER_ERROR, "CDS60003", ErrorInternalServerError.message)
  protected val backendCDS60011SubmissionChannelInvalidResponse: ErrorResponse = ErrorResponse(BAD_REQUEST, "CDS60011", "Invalid declarationSubmissionChannel parameter")

  protected def matchErrorCode(errorCodeText: String): ErrorResponse
  protected def filterResponse(response: HttpResponse, xmlResponseBody: Elem): HttpResponse
  //TODO hmm when did I add the below? should be able to just use the other method
  private def getDateTime: ZonedDateTime = ZonedDateTime.ofInstant(Clock.systemUTC().instant(), ZoneId.of("UTC"))
  //dateTime param allows for testing, never used functionally
  def send[A](searchType: SearchType, dateTime: ZonedDateTime = getDateTime)(implicit asr: AuthorisedRequest[A], hc: HeaderCarrier): Future[Either[Result, HttpResponse]] = {
    val correlationId: CorrelationId = uniqueIdsService.generateUniqueCorrelationId

    futureApiSubFieldsId(asr.clientId).flatMap {
      case Left(result) =>
        Future.successful(Left(result))
      case Right(sfId) =>
        connector.send(dateTime, correlationId, asr.requestedApiVersion, sfId, searchType)
          .map {
            case Right(response) =>
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
                  logger.warn(s"declaration [$endpointName] call failed with backend http status code of [$INTERNAL_SERVER_ERROR] and error: [${errorResponse.errorCode}] so returning to consumer " +
                    s"[${errorResponse.httpStatusCode}] and response body: [${errorResponse.XmlResult.body.asInstanceOf[HttpEntity.Strict].data.utf8String}]")
                  Left(errorResponse.XmlResult.withConversationId)
                case FORBIDDEN =>
                  logger.warn(s"declaration [$endpointName] call failed with backend http status code of [$FORBIDDEN] so returning to consumer [$FORBIDDEN]")
                  Left(ErrorPayloadForbidden.XmlResult.withConversationId)
                case unexpectedStatus =>
                  handleError(s"unexpected backend http status code of [$unexpectedStatus]", INTERNAL_SERVER_ERROR, ErrorInternalServerError)
              }
          }
    }
  }

  private def handleError(message: String, statusToReturn: Int, errorResponse: ErrorResponse)(implicit r: HasConversationId): Left[Result, Nothing] = {
    logger.error(s"declaration[$endpointName] call failed: $message, returning status code [$statusToReturn]")
    Left(errorResponse.XmlResult.withConversationId)
  }
}