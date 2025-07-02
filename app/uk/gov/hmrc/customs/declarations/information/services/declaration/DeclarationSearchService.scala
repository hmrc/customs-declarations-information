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

import play.mvc.Http.Status.BAD_REQUEST
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ErrorInternalServerError
import uk.gov.hmrc.customs.declarations.information.connectors.{ApiSubscriptionFieldsConnector, DeclarationSearchConnector}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.services.UniqueIdsService
import uk.gov.hmrc.customs.declarations.information.services.filter.SearchResponseFilterService
import uk.gov.hmrc.http.HttpResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.xml.Elem

@Singleton
class DeclarationSearchService @Inject()(searchResponseFilterService: SearchResponseFilterService,
                                         override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                         override val logger: InformationLogger,
                                         connector: DeclarationSearchConnector,
                                         uniqueIdsService: UniqueIdsService)(implicit override val ec: ExecutionContext) extends AbstractDeclarationService(apiSubFieldsConnector, logger, connector, uniqueIdsService) {
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
      case _          => ErrorInternalServerError
    }
  }

  protected def filterResponse(response: HttpResponse, xmlResponseBody: Elem): HttpResponse = {
    val responseXml = searchResponseFilterService.findPathThenTransform(xmlResponseBody).head
    HttpResponse(response.status, responseXml.toString(), response.headers)
  }
}
