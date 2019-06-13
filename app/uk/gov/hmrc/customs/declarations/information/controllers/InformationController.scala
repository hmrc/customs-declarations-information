/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declarations.information.controllers

import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes
import play.api.mvc._
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.{AuthAction, ConversationIdAction, ValidateAndExtractHeadersAction}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.Mrn
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{AuthorisedRequest, HasConversationId}
import uk.gov.hmrc.customs.declarations.information.services.DeclarationStatusService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext

@Singleton
class InformationController @Inject()(val validateAndExtractHeadersAction: ValidateAndExtractHeadersAction,
                                      val authAction: AuthAction,
                                      val conversationIdAction: ConversationIdAction,
                                      val declarationStatusService: DeclarationStatusService,
                                      val logger: InformationLogger)
                                     (implicit val ec: ExecutionContext) extends BaseController {

  def get(mrn: String): Action[AnyContent] = (
    Action andThen
      conversationIdAction andThen
      validateAndExtractHeadersAction andThen
      authAction
    ).async {

      implicit asr: AuthorisedRequest[AnyContent] =>

        logger.debug(s"Declaration information request received. Path = ${asr.path} \nheaders = ${asr.headers.headers}")

        declarationStatusService.send(Mrn(mrn)) map {
          case Right(res) =>
            val id = new HasConversationId {
              override val conversationId = asr.conversationId
            }
            logger.info(s"Declaration information request processed successfully.")(id)
            logger.debug(s"Returning filtered declaration status request with status code 200 and body\n ${res.body}")(id)
            Ok(res.body).withConversationId(id).as(ContentTypes.XML)
          case Left(errorResult) =>
            errorResult
        }
    }
}