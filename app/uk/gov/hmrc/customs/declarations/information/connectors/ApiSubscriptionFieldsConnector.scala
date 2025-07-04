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

import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.customs.declarations.information.config.ConfigService
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.ApiSubscriptionFieldsResponse.format
import uk.gov.hmrc.customs.declarations.information.model.{ApiSubscriptionFieldsResponse, ApiSubscriptionKey, HasConversationId}
import uk.gov.hmrc.customs.declarations.information.util.ApiSubscriptionFieldsPath
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiSubscriptionFieldsConnector @Inject()(http: HttpClientV2,
                                               logger: InformationLogger,
                                               config: ConfigService)(implicit ec: ExecutionContext) {
  def getSubscriptionFields(apiSubsKey: ApiSubscriptionKey)(implicit hci: HasConversationId, hc: HeaderCarrier): Future[Option[ApiSubscriptionFieldsResponse]] = {
    val urlPath: String =  ApiSubscriptionFieldsPath.url(config.informationConfig.apiSubscriptionFieldsBaseUrl, apiSubsKey)
    logger.debug(s"Getting fields id from api subscription fields service. url=[$urlPath]")

    http.get(url"$urlPath").execute
      .map { response =>
        response.status match {
          case status if Status.isSuccessful(status) =>
            Json.parse(response.body).asOpt[ApiSubscriptionFieldsResponse] match {
              case Some(value) =>
                Some(value)
              case None =>
                logger.error(s"Could not parse subscription fields response. url=[$urlPath]")
                None
            }
          case status =>
            logger.error(s"Subscriptions fields lookup call failed. url=[$urlPath] HttpStatus=[$status]")
            None
        }
      }
  }
}
