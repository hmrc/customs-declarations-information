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
import uk.gov.hmrc.customs.declarations.information.services.DeclarationVersionService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.text.{ParseException, SimpleDateFormat}
import java.time.ZonedDateTime
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

@Singleton
class SearchController @Inject()(val shutterCheckAction: ShutterCheckAction,
                                 val validateAndExtractHeadersAction: ValidateAndExtractHeadersAction,
                                 val authAction: AuthAction,
                                 val conversationIdAction: ConversationIdAction,
                                 val internalClientIdsCheckAction: InternalClientIdsCheckAction,
                                 val declarationVersionService: DeclarationVersionService,
                                 val cc: ControllerComponents,
                                 val logger: InformationLogger)
                                (implicit val ec: ExecutionContext) extends BackendController(cc) {

  private val validDeclarationCategories = Seq("IM", "EX", "CO", "ALL")
  private val goodsLocationCodeRegex: Regex = "^[a-zA-Z0-9]{1,12}$".r
  private val validDeclarationStatuses = Seq("CLEARED", "UNCLEARED", "REJECTED", "ALL")
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd")

  def list(partyRole: Option[String], declarationCategory: Option[String], goodsLocationCode: Option[String], declarationStatus: Option[String], dateFrom: Option[String], dateTo: Option[String], pageNumber: Option[String], declarationSubmissionChannel: Option[String]): Action[AnyContent] = actionPipeline.async {
    implicit asr: AuthorisedRequest[AnyContent] => search(Mrn(""))
  }

  def validatePartyRole(partyRole: Option[String]):  Either[ErrorResponse, PartyRole] = {

    partyRole.filter(pr => "submitter".compareToIgnoreCase(pr) == 0).map( pr => PartyRole(pr))
      .toRight(errorBadRequest("Invalid partyRole parameter", "CDS60006"))
  };


  def validateDeclarationCategory(declarationCategory: Option[String]):  Either[ErrorResponse, DeclarationCategory] = {
    declarationCategory.filter(dc => validDeclarationCategories.contains(dc.toUpperCase)).map(dc => DeclarationCategory(dc))
      .toRight(errorBadRequest("Invalid declarationCategory parameter", "CDS60008"))
  }

  def validateGoodsLocationCode(goodsLocationCode: Option[String])(implicit request: HasConversationId):  Either[ErrorResponse, Option[GoodsLocationCode]] = {
    goodsLocationCode match {
      case Some(glc) =>
        if (goodsLocationCodeRegex.findFirstIn(glc).nonEmpty) {
          Right(Some(GoodsLocationCode(glc)))
        } else {
          Left(errorBadRequest("Invalid goodsLocationCode parameter", "CDS60010"))
        }
      case None => Right(None)
    }
  }

  def validateDeclarationStatus(declarationStatus: Option[String]): Either[ErrorResponse, Option[DeclarationStatus]] = {

    declarationStatus match {
      case Some(ds) =>
        if (validDeclarationStatuses.contains(ds.toUpperCase)) {
          Right(Some(DeclarationStatus(ds)))
        } else {
          Left(errorBadRequest("Invalid declarationStatus parameter", "CDS60007"))
        }
      case None => Right(None)
    }
  }



  def validateDate(date: Option[String])(implicit request: HasConversationId): Either[ErrorResponse, Option[Date]] = {

    date match {
      case Some(d) => try {
        val dateAsDateType = dateFormat.parse(d)
        Right(Some(dateAsDateType))
      } catch {
        case pe: ParseException =>
          logger.warn(s"Date format incorrect: $d ParseException: $pe")
          Left(ErrorGenericBadRequest)
      }
      case None => Right(None)
    }
  }

  def validatePageNumber(pageNumber: Option[String])(implicit request: HasConversationId): Either[ErrorResponse, Option[Int]] = {
    pageNumber match {
      case Some(pn) => try {
        val validInteger = Integer.parseInt(pn.trim) //TODO can we trim?
        if (validInteger > 0) {
          Right(Some(validInteger))
        } else {
          logger.info(s"pageNumber query parameter was invalid: $pn")
          Left(ErrorGenericBadRequest)
        }
      } catch {
        case nfe: NumberFormatException =>
          logger.info(s"pageNumber query parameter was invalid: $pn exception: $nfe")
          Left(ErrorGenericBadRequest)
      }
      case None => Right(None)
    }
  }

  private def search(mrn: Mrn)(implicit asr: AuthorisedRequest[AnyContent]): Future[Result] = {
    logger.debug(s"Declaration information request received. Path = ${asr.path} \nheaders = ${asr.headers.headers}")

    validateMrn(mrn) match {
      case Right(()) =>
        declarationVersionService.send(Right(mrn)) map {
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
      case Left(result) =>
        Future.successful(result)
    }
  }

  private def validateMrn(mrn: Mrn)(implicit asr: AuthorisedRequest[AnyContent]): Either[Result, Unit] = {
    if(mrn.validValue) {
      Right()
    } else {
      val appropriateResponse = if (mrn.valueTooLong) {
        logger.info(s"MRN parameter is too long: $mrn")
        ErrorResponse(BAD_REQUEST, BadRequestCode, "MRN parameter too long")
      } else if (mrn.valueTooShort) {
        logger.info(s"MRN parameter is missing: $mrn")
        ErrorResponse(BAD_REQUEST, BadRequestCode, "Missing MRN parameter")
      } else {
        logger.info(s"MRN parameter is invalid: $mrn")
        ErrorResponse(BAD_REQUEST, "CDS60002", "MRN parameter invalid")
      }
      Left(appropriateResponse.XmlResult.withConversationId)
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
