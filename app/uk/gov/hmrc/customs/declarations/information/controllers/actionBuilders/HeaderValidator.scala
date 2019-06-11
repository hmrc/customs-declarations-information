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

package uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames.ACCEPT
import play.api.mvc.Headers
import play.mvc.Http.Status.BAD_REQUEST
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declarations.information.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{ConversationIdRequest, ExtractedHeaders, ExtractedHeadersImpl}
import uk.gov.hmrc.customs.declarations.information.model.{BadgeIdentifier, ClientId, VersionOne}

@Singleton
class HeaderValidator @Inject()(logger: InformationLogger) {

  private lazy val xBadgeIdentifierRegex = "^[0-9A-Z]{6,12}$".r
  private val validAcceptHeader: String = "application/vnd.hmrc.1.0+xml"
  private lazy val xClientIdRegex = "^\\S+$".r

  def validateHeaders[A](implicit conversationIdRequest: ConversationIdRequest[A]): Either[ErrorResponse, ExtractedHeaders] = {
    implicit val headers: Headers = conversationIdRequest.headers

    def hasAccept = validateHeader(ACCEPT, validAcceptHeader.equalsIgnoreCase, ErrorAcceptHeaderInvalid)

    def hasXClientId = validateHeader(XClientIdHeaderName, xClientIdRegex.findFirstIn(_).nonEmpty, ErrorInternalServerError)

    def hasBadgeIdentifier = validateHeader(XBadgeIdentifierHeaderName, xBadgeIdentifierRegex.findFirstIn(_).nonEmpty, ErrorResponse(BAD_REQUEST, BadRequestCode, s"$XBadgeIdentifierHeaderName header is missing or invalid"))

    val theResult: Either[ErrorResponse, ExtractedHeaders] = for {
      acceptValue <- hasAccept.right
      xClientIdValue <- hasXClientId.right
      badgeIdentifier <- hasBadgeIdentifier.right
    } yield {
      logger.debug(
        s"\n$ACCEPT header passed validation: $acceptValue"
          + s"\n$XClientIdHeaderName header passed validation: $xClientIdValue"
          + s"\n$XBadgeIdentifierHeaderName header passed validation: $badgeIdentifier")
      ExtractedHeadersImpl(VersionOne, BadgeIdentifier(badgeIdentifier), ClientId(xClientIdValue))
    }
    theResult
  }
  
  private def validateHeader[A](headerName: String, rule: String => Boolean, errorResponse: ErrorResponse)
                                 (implicit conversationIdRequest: ConversationIdRequest[A], h: Headers): Either[ErrorResponse, String] = {
    val left = Left(errorResponse)
    def leftWithLog(headerName: String) = {
      logger.error(s"Error - header '$headerName' not present")
      left
    }
    def leftWithLogContainingValue(headerName: String, value: String) = {
      logger.error(s"Error - header '$headerName' value '$value' is not valid")
      left
    }

    h.get(headerName).fold[Either[ErrorResponse, String]]{
      leftWithLog(headerName)
    }{
      v =>
        if (rule(v)) Right(v) else leftWithLogContainingValue(headerName, v)
    }
  }
  
}
