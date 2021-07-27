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

package uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders

import play.api.http.HttpEntity
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorGenericBadRequest, errorBadRequest}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{HasConversationId, InternalClientIdsRequest, SearchParameters, SearchParametersRequest}
import uk.gov.hmrc.customs.declarations.information.model.{DeclarationCategory, DeclarationStatus, Eori, GoodsLocationCode, PartyRole}
import uk.gov.hmrc.customs.declarations.information.services.InformationConfigService

import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

/** Action builder that checks use of authenticatedParty parameter
 * <ul>
 * <li/>INPUT - `InternalClientIdsRequest`
 * <li/>OUTPUT - `SearchParametersRequest`
 * <li/>ERROR -
 * <ul>
 * <li/>400 if called externally i.e. clientId not present in config
 * </ul>
 */
@Singleton
class SearchParametersCheckAction @Inject()(val logger: InformationLogger,
                                            val configService: InformationConfigService)
                                           (implicit ec: ExecutionContext)
  extends ActionRefiner[InternalClientIdsRequest, SearchParametersRequest] {

  override def executionContext: ExecutionContext = ec

  private val validDeclarationCategories = Seq("IM", "EX", "CO", "ALL")
  private val goodsLocationCodeRegex: Regex = "^[a-zA-Z0-9]{1,12}$".r
  private val looseEoriRegex: Regex = "^[a-zA-Z0-9]{1,50}$".r
  private val validDeclarationStatuses = Seq("CLEARED", "UNCLEARED", "REJECTED", "ALL")
  private val validPartyRoles = Seq("SUBMITTER", "CONSIGNEE", "CONSIGNOR", "DECLARANT")
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd")

  override def refine[A](icr: InternalClientIdsRequest[A]): Future[Either[Result, SearchParametersRequest[A]]] = Future.successful {
    implicit val id: InternalClientIdsRequest[A] = icr

    val maybeEori = icr.request.getQueryString("eori")
    val maybePartyRole = icr.request.getQueryString("partyRole")
    val maybeDeclarationCategory = icr.request.getQueryString("declarationCategory")
    val maybeGoodsLocationCode = icr.request.getQueryString("goodsLocationCode")
    val maybeDeclarationStatus = icr.request.getQueryString("declarationStatus")
    val maybeDateFrom = icr.request.getQueryString("dateFrom")
    val maybeDateTo = icr.request.getQueryString("dateTo")
    val maybePageNumber = icr.request.getQueryString("pageNumber")

  val searchParameters: Either[ErrorResponse, SearchParametersRequest[A]] = for {
    eori <- validateEori(maybeEori).right
    partyRole <- validatePartyRole(maybePartyRole).right
    declarationCategory <- validateDeclarationCategory(maybeDeclarationCategory).right
    goodsLocationCode <- validateGoodsLocationCode(maybeGoodsLocationCode).right
    declarationStatus <- validateDeclarationStatus(maybeDeclarationStatus).right
    dateFrom <- validateDate(maybeDateFrom).right
    dateTo <- validateDate(maybeDateTo).right
    dateChronology <- validateDateChronology(dateFrom, dateTo)
    pageNumber <- validatePageNumber(maybePageNumber).right
  } yield SearchParametersRequest(icr.conversationId, icr.requestedApiVersion, icr.clientId, icr.declarationSubmissionChannel,
    Some(SearchParameters(eori, partyRole, declarationCategory, goodsLocationCode, declarationStatus, dateFrom, dateTo, pageNumber)), icr.request)

    if(searchParameters.isLeft) {
      val error = searchParameters.left.get
      logger.warn(s"Rejected declaration information search request with status code ${error.httpStatusCode} and body\n ${error.XmlResult.body.asInstanceOf[HttpEntity.Strict].data.utf8String}")
      Left(error.XmlResult.withConversationId)
    }  else {
      Right(searchParameters.right.get)
    }
  }

  def validateEori(maybeEori: Option[String])(implicit request: HasConversationId):  Either[ErrorResponse, Option[Eori]] = {
    maybeEori match {
      case Some(eori) =>
        if (looseEoriRegex.findFirstIn(eori).nonEmpty) {
          Right(Some(Eori(eori)))
        } else {
          logger.info(s"eori query parameter was invalid: $eori")
          Left(ErrorGenericBadRequest)
        }
      case None => Right(None)
    }
  }

  def validatePartyRole(partyRole: Option[String]):  Either[ErrorResponse, PartyRole] = {

    partyRole.filter(pr => validPartyRoles.contains(pr.toUpperCase)).map( pr => PartyRole(pr))
      .toRight(errorBadRequest("Invalid partyRole parameter", "CDS60006"))
  }


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
          logger.info(s"goodsLocationCode query parameter was invalid: $glc")
          Left(errorBadRequest("Invalid goodsLocationCode parameter", "CDS60010"))
        }
      case None => Right(None)
    }
  }

  def validateDeclarationStatus(declarationStatus: Option[String])(implicit request: HasConversationId): Either[ErrorResponse, Option[DeclarationStatus]] = {

    declarationStatus match {
      case Some(ds) =>
        if (validDeclarationStatuses.contains(ds.toUpperCase)) {
          Right(Some(DeclarationStatus(ds)))
        } else {
          logger.info(s"declarationStatus query parameter was invalid: $ds")
          Left(errorBadRequest("Invalid declarationStatus parameter", "CDS60007"))
        }
      case None => Right(None)
    }
  }
  def validateDateChronology(dateFrom: Option[Date], dateTo: Option[Date])(implicit request: HasConversationId): Either[ErrorResponse, Unit] = {
    if (dateFrom.isDefined && dateTo.isDefined && dateTo.get.before(dateFrom.get)) {
        Left(errorBadRequest("Invalid date parameters", "CDS60009"))
    } else {
      Right()
    }
  }

  def validateDate(date: Option[String])(implicit request: HasConversationId): Either[ErrorResponse, Option[Date]] = {

    date match {
      case Some(d) => try {
        val dateAsDateType = dateFormat.parse(d)
        if(dateAsDateType.compareTo(new Date()) <= 0) {
          Right(Some(dateAsDateType))
        } else {
          logger.info(s"Date was in the future: $d")
          Left(errorBadRequest("Invalid date parameters", "CDS60009"))
        }
      } catch {
        case pe: ParseException =>
          logger.info(s"Date format incorrect: $d ParseException: $pe")
          Left(errorBadRequest("Invalid date parameters", "CDS60009"))
      }
      case None => Right(None)
    }
  }

  def validatePageNumber(pageNumber: Option[String])(implicit request: HasConversationId): Either[ErrorResponse, Option[Int]] = {
    pageNumber match {
      case Some(pn) => try {
        val validInteger = Integer.parseInt(pn.trim)
        if (validInteger > 0) {
          Right(Some(validInteger))
        } else {
          logger.info(s"pageNumber query parameter was invalid: $pn")
          Left(errorBadRequest("Invalid pageNumber parameter", "CDS60012"))
        }
      } catch {
        case nfe: NumberFormatException =>
          logger.info(s"pageNumber query parameter was invalid: $pn exception: $nfe")
          Left(errorBadRequest("Invalid pageNumber parameter", "CDS60012"))
      }
      case None => Right(None)
    }
  }
}
