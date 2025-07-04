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

package uk.gov.hmrc.customs.declarations.information.logging

import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.api.mvc.Request
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.util.CustomHeaderNames._

object LoggingHelper {
  private val headerSet = Set(CONTENT_TYPE.toLowerCase, ACCEPT.toLowerCase, XConversationIdHeaderName.toLowerCase, XClientIdHeaderName.toLowerCase, XBadgeIdentifierHeaderName.toLowerCase)

  def formatError(msg: String, r: HasConversationId): String = {
    formatMessage(msg, r)
  }

  def formatWarn(msg: String, r: HasConversationId): String = {
    formatMessage(msg, r)
  }

  def formatInfo(msg: String, r: HasConversationId): String = {
    formatMessage(msg, r)
  }

  def formatDebug(msg: String, r: HasConversationId): String = {
    formatMessage(msg, r)
  }

  def formatDebugFull(msg: String, r: HasConversationId with Request[Any]): String = {
    formatMessageFull(msg, r)
  }

  private def formatMessage(msg: String, r: HasConversationId): String = {
    s"${format(r)} $msg".trim
  }

  private def format(r: HasConversationId): String = {
    def conversationId = s"[conversationId=${r.conversationId}]"

    def apiVersion: String = r match {
      case a: HasApiVersion => s"[requestedApiVersion=${a.requestedApiVersion}]"
      case _ => ""
    }

    def extractedHeaders: String = r match {
      case h: ExtractedHeaders => s"[clientId=${h.clientId}]"
      case _ => ""
    }

    def authorised: String = r match {
      case a: HasAuthorisedAs =>
        a.authorisedAs match {
          case NonCsp(eori) => s"[authorisedAs=NonCsp($eori)]"
          case Csp(eori, badgeIdentifier) => s"[authorisedAs=Csp($eori, $badgeIdentifier)]"
        }
      case _ => ""
    }

    s"$conversationId$extractedHeaders$apiVersion$authorised"
  }

  def formatMessageFull(msg: String, r: HasConversationId with Request[Any]): String = {
    def filteredHeaders: Map[String, String] = r.headers.toSimpleMap.filter(keyValTuple =>
      headerSet.contains(keyValTuple._1.toLowerCase))

    s"[conversationId=${r.conversationId.uuid}] $msg headers=$filteredHeaders"
  }
}
