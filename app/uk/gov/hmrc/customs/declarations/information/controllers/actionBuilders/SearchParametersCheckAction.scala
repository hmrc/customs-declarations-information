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

import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorGenericBadRequest, errorBadRequest}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.{DeclarationCategory, DeclarationStatus, DeclarationSubmissionChannel, GoodsLocationCode, PartyRole}
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{HasConversationId, InternalClientIdsRequest, SearchParameters, SearchParametersRequest, ValidatedHeadersRequest}
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
  private val validDeclarationStatuses = Seq("CLEARED", "UNCLEARED", "REJECTED", "ALL")
  private val validDeclarationSubmissionChannel = Seq("AuthenticatedPartyOnly")
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd")

  val declarationSubmissionChannelErrorCode = "CDS60011"

  override def refine[A](icr: InternalClientIdsRequest[A]): Future[Either[Result, SearchParametersRequest[A]]] = Future.successful {
    implicit val id: InternalClientIdsRequest[A] = icr

    val maybePartyRole = icr.request.getQueryString("declarationSubmissionChannel")
    val maybeDeclarationCategory = icr.request.getQueryString("declarationSubmissionChannel")
    val maybeGoodsLocationCode = icr.request.getQueryString("maybeDoodsLocationCode")
    val maybeDeclarationStatus = icr.request.getQueryString("declarationStatus")
    val maybeDateFrom = icr.request.getQueryString("dateFrom")
    val maybeDateTo = icr.request.getQueryString("dateTo")
    val maybePageNumber = icr.request.getQueryString("pageNumber")
    val maybeDeclarationSubmissionChannel = icr.request.getQueryString("declarationSubmissionChannel")

  val searchParameters: Either[ErrorResponse, SearchParametersRequest[A]] = for {
    partyRole <- validatePartyRole(maybePartyRole).right
    declarationCategory <- validateDeclarationCategory(maybeDeclarationCategory).right
    goodsLocationCode <- validateGoodsLocationCode(maybeGoodsLocationCode).right
    declarationStatus <- validateDeclarationStatus(maybeDeclarationStatus).right
    dateFrom <- validateDate(maybeDateFrom).right
    dateTo <- validateDate(maybeDateTo).right
    pageNumber <- validatePageNumber(maybePageNumber).right
  } yield SearchParametersRequest(icr.conversationId, icr.requestedApiVersion, icr.clientId, icr.declarationSubmissionChannel,
    Some(SearchParameters(partyRole, declarationCategory, goodsLocationCode, declarationStatus, dateFrom, dateTo, pageNumber)), icr.request)

    if(searchParameters.isLeft) {
      Left(searchParameters.left.get.XmlResult.withConversationId)
    }  else {
      Right(searchParameters.right.get)
    }
  }

  def validatePartyRole(partyRole: Option[String]):  Either[ErrorResponse, PartyRole] = {

    partyRole.filter(pr => "submitter".compareToIgnoreCase(pr) == 0).map( pr => PartyRole(pr))
      .toRight(errorBadRequest("Invalid maybePartyRole parameter", "CDS60006"))
  }


  def validateDeclarationCategory(declarationCategory: Option[String]):  Either[ErrorResponse, DeclarationCategory] = {
    declarationCategory.filter(dc => validDeclarationCategories.contains(dc.toUpperCase)).map(dc => DeclarationCategory(dc))
      .toRight(errorBadRequest("Invalid maybeDeclarationCategory parameter", "CDS60008"))
  }

  def validateGoodsLocationCode(goodsLocationCode: Option[String])(implicit request: HasConversationId):  Either[ErrorResponse, Option[GoodsLocationCode]] = {
    goodsLocationCode match {
      case Some(glc) =>
        if (goodsLocationCodeRegex.findFirstIn(glc).nonEmpty) {
          Right(Some(GoodsLocationCode(glc)))
        } else {
          Left(errorBadRequest("Invalid maybeDoodsLocationCode parameter", "CDS60010"))
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
}
