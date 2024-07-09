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

import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ErrorInternalServerError
import uk.gov.hmrc.customs.declarations.information.config.ConfigService
import uk.gov.hmrc.customs.declarations.information.connectors.{ApiSubscriptionFieldsConnector, DeclarationVersionConnector}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.services.UniqueIdsService
import uk.gov.hmrc.customs.declarations.information.services.filter.VersionResponseFilterService
import uk.gov.hmrc.http.HttpResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.xml.Elem

@Singleton
class DeclarationVersionService @Inject()(versionResponseFilterService: VersionResponseFilterService,
                                          override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                          override val logger: InformationLogger,
                                          connector: DeclarationVersionConnector,
                                          uniqueIdsService: UniqueIdsService,
                                          config: ConfigService)(implicit override val ec: ExecutionContext) extends AbstractDeclarationService(apiSubFieldsConnector, logger, connector, uniqueIdsService) {
  protected val endpointName: String = "version"

  protected def matchErrorCode(errorCodeText: String): ErrorResponse = {
    errorCodeText.toLowerCase() match {
      case "cds60001" => backendCDS60001NotFoundResponse
      case "cds60002" => backendCDS60002MrnInvalidResponse
      case "cds60003" => backendCDS60003InternalServerErrorResponse
      case "cds60011" => backendCDS60011SubmissionChannelInvalidResponse
      case _          => ErrorInternalServerError
    }
  }

  protected def filterResponse(response: HttpResponse, xmlResponseBody: Elem): HttpResponse = {
    val responseXml = versionResponseFilterService.findPathThenTransform(xmlResponseBody).head
    HttpResponse(response.status, responseXml.toString(), response.headers)
  }
}
