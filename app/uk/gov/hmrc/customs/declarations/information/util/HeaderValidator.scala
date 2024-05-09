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

package uk.gov.hmrc.customs.declarations.information.util

import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.util.CustomHeaderNames._

import javax.inject.{Inject, Singleton}
import scala.util.matching.Regex

//TODO probably can be an object too, just pass the logger into the method
@Singleton
class HeaderValidator @Inject()(logger: InformationLogger) {
  def extractClientIdHeaderIfPresentAndValid[A](implicit apiVersionRequest: ApiVersionRequest[A]): Either[ErrorResponse, ExtractedHeaders] = {
    apiVersionRequest.headers.get(XClientIdHeaderName) match {
      case Some(clientIdHeaderValue) if clientIdHeaderValue.isEmpty || clientIdHeaderValue.contains(" ") =>
        logger.error(s"Error - header '$XClientIdHeaderName' value '$clientIdHeaderValue' is not valid")
        Left(ErrorInternalServerError)
      case Some(clientIdHeaderValue) =>
        logger.debug(s"\n$XClientIdHeaderName header passed validation: $clientIdHeaderValue")
        Right(ExtractedHeadersImpl(ClientId(clientIdHeaderValue)))
      case None =>
        logger.error(s"Error - header '$XClientIdHeaderName' not present")
        Left(ErrorInternalServerError)
    }
  }

  //TODO I refactored the above heavily and I'm sure the below can be too
  def eitherBadgeIdentifier[A](allowNone: Boolean)(implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[BadgeIdentifier]] = {
    val xBadgeIdentifierRegex: Regex = "^[0-9A-Z]{6,12}$".r
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
        errorBadRequest(s"$XBadgeIdentifierHeaderName header is missing or invalid")
      }
    }
  }

  def eoriMustBeValidIfPresent[A](implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[Eori]] = {
    val maybeEoriHeader: Option[String] = vhr.request.headers.toSimpleMap.get(XSubmitterIdentifierHeaderName)
    logger.debug(s"maybeEori => $maybeEoriHeader")
    val maybeEori: Option[String] = convertEmptyHeaderToNone(maybeEoriHeader)

    maybeEori match {
      case Some(eori) => if (validEori(eori)) {
        logger.info(s"$XSubmitterIdentifierHeaderName header passed validation: $eori")
        Right(Some(Eori(eori)))
      } else {
        logger.error(s"$XSubmitterIdentifierHeaderName header is invalid for CSP: $eori")
        Left(errorBadRequest(s"$XSubmitterIdentifierHeaderName header is invalid"))
      }
      case None =>
        logger.info(s"$XSubmitterIdentifierHeaderName header not present or is empty")
        Right(None)
    }
  }

  private def validEori(eori: String): Boolean = {
    val InvalidEoriHeaderRegex = "(^[\\s]*$|^.{18,}$)".r
    InvalidEoriHeaderRegex.findFirstIn(eori).isEmpty
  }

  private def convertEmptyHeaderToNone(eori: Option[String]): Option[String] = {
    if (eori.isDefined && eori.get.trim.isEmpty) {
      eori map (_.trim) filterNot (_.isEmpty)
    } else {
      eori
    }
  }
}
