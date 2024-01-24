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

import play.api.mvc.Result
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorInternalServerError
import uk.gov.hmrc.customs.declarations.information.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.http.HeaderCarrier

import java.net.URLEncoder
import scala.concurrent.{ExecutionContext, Future}

trait ApiSubscriptionFieldsService {

  def apiSubFieldsConnector: ApiSubscriptionFieldsConnector

  def logger: InformationLogger

  implicit def ec: ExecutionContext

  private val apiContextEncoded = URLEncoder.encode("customs/declarations-information", "UTF-8")

  def futureApiSubFieldsId[A](c: ClientId)
                             (implicit asr: AuthorisedRequest[A], hc: HeaderCarrier): Future[Either[Result, Option[ApiSubscriptionFieldsResponse]]] = {
    asr.authorisedAs match {
      case Csp(_, _) =>
        apiSubFieldsConnector.getSubscriptionFields(ApiSubscriptionKey(c, apiContextEncoded, asr.requestedApiVersion))
          .map {
            case Some(response) =>
              if (response.fields.authenticatedEori.exists(!_.isBlank)) {
                Right(Some(response))
              } else {
                val msg = "Missing authenticated eori in service lookup"
                logger.warn(msg)
                Left(errorInternalServerError(msg).XmlResult.withConversationId)
              }
            case None =>
              Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
          }
      case NonCsp(_) => Future.successful(Right(None))
    }
  }
}
