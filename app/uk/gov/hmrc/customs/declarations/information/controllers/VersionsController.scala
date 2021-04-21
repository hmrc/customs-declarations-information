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

package uk.gov.hmrc.customs.declarations.information.controllers

import play.api.http.ContentTypes
import play.api.mvc._
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders._
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{AuthorisedRequest, HasConversationId}
import uk.gov.hmrc.customs.declarations.information.services.DeclarationVersionsService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VersionsController @Inject()(val shutterCheckAction: ShutterCheckAction,
                                   val validateAndExtractHeadersAction: ValidateAndExtractHeadersAction,
                                   val authAction: AuthAction,
                                   val conversationIdAction: ConversationIdAction,
                                   val internalClientIdsCheckAction: InternalClientIdsCheckAction,
                                   val declarationVersionsService: DeclarationVersionsService,
                                   val cc: ControllerComponents,
                                   val logger: InformationLogger)
                                  (implicit val ec: ExecutionContext) extends BackendController(cc) {

  def list(mrn: String, authenticatedParty: Option[Boolean]): Action[AnyContent] = actionPipeline.async {
    val mrn1 = Mrn(mrn)
    implicit asr: AuthorisedRequest[AnyContent] => search(mrn1)
  }

  private def search(mrn: Mrn)(implicit asr: AuthorisedRequest[AnyContent]): Future[Result] = {
    logger.debug(s"Declaration information request received. Path = ${asr.path} \nheaders = ${asr.headers.headers}")

      if (!mrn.validValue)
        logger.warn(s"Invalid versions call for MRN: ${mrn.value}")

        val appropriateResponse = if (mrn.valueTooLong) {
          ErrorResponse(BAD_REQUEST, BadRequestCode, "Search parameter too long")
        } else if(mrn.valueTooShort) {
          ErrorResponse(BAD_REQUEST, BadRequestCode, "Missing search parameter")
        } else {
          ErrorResponse(BAD_REQUEST, "CDS60002", "Search parameter invalid")
        }

        Future.successful(appropriateResponse.XmlResult.withConversationId)

        declarationVersionsService.send(mrn) map {
          case Right(res: HttpResponse) =>
            new HasConversationId {
              override val conversationId = asr.conversationId
            }
            logger.info(s"Declaration information versions processed successfully.")
            logger.debug(s"Returning declaration information versions response with status code ${res.status} and body\n ${res.body}")
            Ok(res.body).withConversationId.as(ContentTypes.XML)
          case Left(errorResult) =>
            errorResult
        }
  }

  private val actionPipeline: ActionBuilder[AuthorisedRequest, AnyContent] =
    Action andThen
      conversationIdAction andThen
      shutterCheckAction andThen
      validateAndExtractHeadersAction andThen
      internalClientIdsCheckAction andThen
      authAction
}
