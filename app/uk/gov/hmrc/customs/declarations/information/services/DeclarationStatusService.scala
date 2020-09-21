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

package uk.gov.hmrc.customs.declarations.information.services

import akka.pattern.CircuitBreakerOpenException
import javax.inject.{Inject, Singleton}
import play.api.mvc.Result
import play.mvc.Http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{NotFoundCode, errorInternalServerError}
import uk.gov.hmrc.customs.declarations.information.connectors.{ApiSubscriptionFieldsConnector, DeclarationStatusConnector, Non2xxResponseException}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.SearchType
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left
import scala.util.control.NonFatal
import scala.xml.{Elem, NodeSeq, PrettyPrinter, TopScope, XML}

@Singleton
class DeclarationStatusService @Inject()(statusResponseFilterService: StatusResponseFilterService,
                                         override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                         override val logger: InformationLogger,
                                         connector: DeclarationStatusConnector,
                                         dateTimeProvider: DateTimeService,
                                         uniqueIdsService: UniqueIdsService)
                                        (implicit val ec: ExecutionContext) extends ApiSubscriptionFieldsService {

  def send[A](searchType: SearchType)(implicit asr: AuthorisedRequest[A], hc: HeaderCarrier): Future[Either[Result, HttpResponse]] = {

    val dateTime = dateTimeProvider.nowUtc()
    val correlationId = uniqueIdsService.correlation

    futureApiSubFieldsId(asr.clientId) flatMap {
      case Right(sfId) =>
        connector.send(dateTime, correlationId, asr.requestedApiVersion, sfId, searchType)
          .map(response => {
            val xmlResponseBody = XML.loadString(response.body)
            Right(filterResponse(response, xmlResponseBody))
          }).recover{
          case e: Non2xxResponseException if e.responseCode == INTERNAL_SERVER_ERROR =>
            val body = XML.loadString(e.response.body)
            val errorCode: NodeSeq = body \ "errorCode"
            val errorCodeText = errorCode.text
            logger.warn(s"declaration status call failed with 500: $errorCodeText")
            matchErrorCode(errorCodeText)
          case e: Non2xxResponseException if e.responseCode == FORBIDDEN =>
            logger.warn(s"declaration status call failed with 403: ${e.getMessage}")
            Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
          case e: HttpException if e.responseCode == NOT_FOUND =>
            logger.warn(s"declaration status call failed with 404: ${e.getMessage}")
            Left(DeclarationStatusService.customNotFoundResponse.XmlResult.withConversationId)
          case e: HttpException =>
            logger.warn(s"declaration status call failed with ${e.responseCode}: ${e.getMessage}")
            Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
          case _: CircuitBreakerOpenException =>
            logger.error("unhealthy state entered")
            Left(errorResponseServiceUnavailable.XmlResult.withConversationId)
          case NonFatal(e) =>
            logger.error(s"declaration status call failed: ${e.getMessage}", e)
            Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
        }
      case Left(result) =>
        Future.successful(Left(result))
    }
  }


  private def matchErrorCode[A](errorCodeText: String)(implicit asr: AuthorisedRequest[A], hc: HeaderCarrier): Either[Result, HttpResponse] = {
    errorCodeText.toLowerCase() match {
      case "cds60001" =>
        Left(ErrorResponse(NOT_FOUND, errorCodeText, "Declaration not found").XmlResult.withConversationId)
      case "cds60002" =>
        Left(ErrorResponse(BAD_REQUEST, errorCodeText, "Search parameter invalid").XmlResult.withConversationId)
      case "cds60003" =>
        Left(ErrorResponse(INTERNAL_SERVER_ERROR, errorCodeText, "Internal server error").XmlResult.withConversationId)
      case _ =>
        Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

  private def filterResponse(response: HttpResponse, xmlResponseBody: Elem): HttpResponse = {
    val xmlWidth = 120
    val xmlIndent = 2

    val statusResponseXml = statusResponseFilterService.transform(xmlResponseBody).head
    val statusResponseString = new PrettyPrinter(xmlWidth, xmlIndent).format(statusResponseXml, TopScope)

    HttpResponse(response.status, statusResponseString, response.headers)
  }

  private val errorResponseServiceUnavailable = errorInternalServerError("This service is currently unavailable")
}

object DeclarationStatusService {
  val customNotFoundResponse = ErrorResponse(NOT_FOUND, NotFoundCode, "Declaration Not Found")
}
