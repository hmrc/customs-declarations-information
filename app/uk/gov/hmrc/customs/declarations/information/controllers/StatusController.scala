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

import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes
import play.api.mvc._
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.{AuthAction, ConversationIdAction, InternalClientIdsCheckAction, ShutterCheckAction, ValidateAndExtractHeadersAction}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{AuthorisedRequest, HasConversationId}
import uk.gov.hmrc.customs.declarations.information.services.DeclarationStatusService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatusController @Inject()(val shutterCheckAction: ShutterCheckAction,
                                 val validateAndExtractHeadersAction: ValidateAndExtractHeadersAction,
                                 val internalClientIdsCheckAction: InternalClientIdsCheckAction,
                                 val authAction: AuthAction,
                                 val conversationIdAction: ConversationIdAction,
                                 val declarationStatusService: DeclarationStatusService,
                                 val cc: ControllerComponents,
                                 val logger: InformationLogger)
                                (implicit val ec: ExecutionContext) extends BackendController(cc) {

  def getByMrn(mrn: String): Action[AnyContent] = actionPipeline.async {
    val searchType = Mrn(mrn)
    implicit asr: AuthorisedRequest[AnyContent] => search(searchType)
  }

  def getByDucr(ducr: String): Action[AnyContent] = actionPipeline.async {
    val searchType = Ducr(ducr)
    implicit asr: AuthorisedRequest[AnyContent] => search(searchType)
  }

  def getByUcr(ucr: String): Action[AnyContent] = actionPipeline.async {
    val searchType = Ucr(ucr)
    implicit asr: AuthorisedRequest[AnyContent] => search(searchType)
  }

  def getByInventoryReference(inventoryReference: String): Action[AnyContent] = actionPipeline.async {
    val searchType = InventoryReference(inventoryReference)
    implicit asr: AuthorisedRequest[AnyContent] => search(searchType)
  }

  private def search(searchType: SearchType)(implicit asr: AuthorisedRequest[AnyContent]): Future[Result] = {
    logger.debug(s"Declaration information request received. Path = ${asr.path} \nheaders = ${asr.headers.headers}")

    searchType match {
      case s: SearchType if !s.validValue =>
        logger.warn(s"Invalid search for ${searchType.label}: ${searchType.value}")

        val appropriateResponse = if (s.valueTooLong) {
          ErrorResponse(BAD_REQUEST, BadRequestCode, "Search parameter too long")
        } else if(s.valueTooShort) {
          ErrorResponse(BAD_REQUEST, BadRequestCode, "Missing search parameter")
        } else {
          ErrorResponse(BAD_REQUEST, "CDS60002", "Search parameter invalid")
        }

        Future.successful(appropriateResponse.XmlResult.withConversationId)

      case _: Mrn | _: Ducr | _: Ucr | _: InventoryReference =>
        declarationStatusService.send(Left(searchType)) map {
          case Right(res: HttpResponse) =>
            new HasConversationId {
              override val conversationId = asr.conversationId
            }
            logger.info(s"Declaration information request by ${searchType.getClass.getSimpleName} processed successfully.")
            logger.debug(s"Returning filtered declaration status request with status code ${res.status} and body\n ${res.body}")
            Ok(res.body).withConversationId.as(ContentTypes.XML)
          case Left(errorResult) =>
            errorResult
        }
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
