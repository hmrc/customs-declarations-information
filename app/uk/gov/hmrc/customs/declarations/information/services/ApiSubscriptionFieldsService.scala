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

import java.net.URLEncoder

import play.api.mvc.Result
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorInternalServerError
import uk.gov.hmrc.customs.declarations.information.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left
import scala.util.control.NonFatal

trait ApiSubscriptionFieldsService {

  def apiSubFieldsConnector: ApiSubscriptionFieldsConnector

  def logger: InformationLogger

  implicit def ec: ExecutionContext

  private val apiContextEncoded = URLEncoder.encode("customs/declarations-information", "UTF-8")

  def futureApiSubFieldsId[A](c: ClientId)
                             (implicit asr: AuthorisedRequest[A], hc: HeaderCarrier): Future[Either[Result, Option[ApiSubscriptionFieldsResponse]]] = {
    asr.authorisedAs match {
      case Csp(_, _) =>
        (apiSubFieldsConnector.getSubscriptionFields(ApiSubscriptionKey(c, apiContextEncoded, asr.requestedApiVersion)) map { response =>
          if (validAuthenticatedEori(response.fields.authenticatedEori)) {
            Right(Some(response))
          } else {
            val msg = "Missing authenticated eori in service lookup"
            logger.warn(msg)
            Left(errorInternalServerError(msg).XmlResult.withConversationId)
          }
      }).recover {
        case NonFatal(e) =>
          logger.error(s"Subscriptions fields lookup call failed: ${e.getMessage}", e)
          Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
      }
      case NonCsp(_) => Future.successful(Right(None))
    }

  }

  private def validAuthenticatedEori(authenticatedEori: Option[String]): Boolean = {
    if (authenticatedEori.isDefined && !authenticatedEori.get.trim.isEmpty) {
      true
    } else {
      false
    }
  }
}
