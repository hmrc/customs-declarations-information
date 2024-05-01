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

package uk.gov.hmrc.customs.declarations.information.action

import play.api.mvc.Headers
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declarations.information.util.CustomHeaderNames._
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.{ApiVersionRequest, BadgeIdentifier, ClientId, Eori, ExtractedHeaders, ExtractedHeadersImpl, HasConversationId, HasRequest}

import javax.inject.{Inject, Singleton}

@Singleton
class HeaderValidator @Inject()(logger: InformationLogger) {
  private lazy val xBadgeIdentifierRegex = "^[0-9A-Z]{6,12}$".r
  private lazy val xClientIdRegex = "^\\S+$".r
  private lazy val InvalidEoriHeaderRegex = "(^[\\s]*$|^.{18,}$)".r
  private val errorResponseBadgeIdentifierHeaderMissing: ErrorResponse = errorBadRequest(s"$XBadgeIdentifierHeaderName header is missing or invalid")
  private val errorResponseEoriIdentifierHeaderInvalid: ErrorResponse = errorBadRequest(s"$XSubmitterIdentifierHeaderName header is invalid")

  def validateHeaders[A](implicit apiVersionRequest: ApiVersionRequest[A]): Either[ErrorResponse, ExtractedHeaders] = {
    implicit val headers: Headers = apiVersionRequest.headers
      //TODO internal def
    def hasXClientId: Either[ErrorResponse, String] = validateHeader(XClientIdHeaderName, xClientIdRegex.findFirstIn(_).nonEmpty, ErrorInternalServerError)

    val theResult: Either[ErrorResponse, ExtractedHeaders] = for {
      xClientIdValue <- hasXClientId
    } yield {
      logger.debug(
        s"\n$XClientIdHeaderName header passed validation: $xClientIdValue")
      ExtractedHeadersImpl(ClientId(xClientIdValue))
    }
    theResult
  }

  private def validateHeader[A](headerName: String,
                                rule: String => Boolean,
                                errorResponse: ErrorResponse)(implicit apiVersionRequest: ApiVersionRequest[A], headers: Headers): Either[ErrorResponse, String] = {
    val left: Left[ErrorResponse, Nothing] = Left(errorResponse)
//TODO internal defs
    def leftWithLog(headerName: String) = {
      logger.error(s"Error - header '$headerName' not present")
      left
    }

    def leftWithLogContainingValue(headerName: String, value: String) = {
      logger.error(s"Error - header '$headerName' value '$value' is not valid")
      left
    }

    headers.get(headerName).fold[Either[ErrorResponse, String]] {
      leftWithLog(headerName)
    } {
          //TODO what is v?
      v =>
        if (rule(v)) Right(v) else leftWithLogContainingValue(headerName, v)
    }
  }

  def eitherBadgeIdentifier[A](allowNone: Boolean)(implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[BadgeIdentifier]] = {
    val maybeBadgeId: Option[String] = vhr.request.headers.toSimpleMap.get(XBadgeIdentifierHeaderName)

    if (allowNone && maybeBadgeId.isEmpty) {
      logger.info(s"$XBadgeIdentifierHeaderName header empty and allowed")
      Right(None)
    } else {
      maybeBadgeId.filter(xBadgeIdentifierRegex.findFirstIn(_).nonEmpty).map(b => {
        logger.info(s"$XBadgeIdentifierHeaderName header passed validation: $b")
        Some(BadgeIdentifier(b))
      }
      ).toRight[ErrorResponse] {
        logger.error(s"$XBadgeIdentifierHeaderName invalid or not present for CSP")
        errorResponseBadgeIdentifierHeaderMissing
      }
    }
  }

  def eoriMustBeValidIfPresent[A](implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[Eori]] = {
    val maybeEoriHeader: Option[String] = vhr.request.headers.toSimpleMap.get(XSubmitterIdentifierHeaderName)
    logger.debug(s"maybeEori => $maybeEoriHeader")
    val maybeEori = convertEmptyHeaderToNone(maybeEoriHeader)

    maybeEori match {
      case Some(eori) => if (validEori(eori)) {
        logger.info(s"$XSubmitterIdentifierHeaderName header passed validation: $eori")
        Right(Some(Eori(eori)))
      } else {
        logger.error(s"$XSubmitterIdentifierHeaderName header is invalid for CSP: $eori")
        Left(errorResponseEoriIdentifierHeaderInvalid)
      }
      case None =>
        logger.info(s"$XSubmitterIdentifierHeaderName header not present or is empty")
        Right(None)
    }
  }

  private def validEori(eori: String): Boolean = InvalidEoriHeaderRegex.findFirstIn(eori).isEmpty

  private def convertEmptyHeaderToNone(eori: Option[String]): Option[String] = {
    if (eori.isDefined && eori.get.trim.isEmpty) {
      eori map (_.trim) filterNot (_.isEmpty)
    } else {
      eori
    }
  }
}
