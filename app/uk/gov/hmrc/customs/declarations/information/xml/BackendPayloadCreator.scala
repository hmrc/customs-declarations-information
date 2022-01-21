/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declarations.information.xml

import org.joda.time.DateTime
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest

import scala.xml.{NodeSeq, Text}

trait BackendPayloadCreator {

  private val newLineAndIndentation = "\n        "

  def create[A](conversationId: ConversationId,
                correlationId: CorrelationId,
                date: DateTime,
                searchType: SearchType,
                maybeApiSubscriptionFieldsResponse: Option[ApiSubscriptionFieldsResponse])
               (implicit asr: AuthorisedRequest[A]): NodeSeq


  def requestCommon[A](conversationId: ConversationId,
                    correlationId: CorrelationId,
                    date: DateTime,
                    searchType: SearchType,
                    maybeApiSubscriptionFieldsResponse: Option[ApiSubscriptionFieldsResponse])
                   (implicit asr: AuthorisedRequest[A]): NodeSeq =
    <n1:requestCommon>
    {Seq[NodeSeq](
      <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>, Text(newLineAndIndentation),
      <n1:conversationID>{conversationId.toString}</n1:conversationID>, Text(newLineAndIndentation),
      <n1:correlationID>{correlationId.toString}</n1:correlationID>)}
    {val as = asr.authorisedAs
    as match {
      case NonCsp(eori) => Seq[NodeSeq](
        <n1:dateTimeStamp>{date.toString()}</n1:dateTimeStamp>, Text(newLineAndIndentation),
        <n1:authenticatedPartyID>{eori.toString}</n1:authenticatedPartyID>)
      case Csp(_, badgeId) =>
        Seq[NodeSeq](
          badgeId.fold(NodeSeq.Empty)(badge => <n1:badgeIdentifier>{badge.toString}</n1:badgeIdentifier>), Text(newLineAndIndentation),
          <n1:dateTimeStamp>{date.toString}</n1:dateTimeStamp>, Text(newLineAndIndentation),
          <n1:authenticatedPartyID>{maybeApiSubscriptionFieldsResponse.get.fields.authenticatedEori.get}</n1:authenticatedPartyID>, Text(newLineAndIndentation),
          <n1:originatingPartyID>{Csp.originatingPartyId(as.asInstanceOf[Csp])}</n1:originatingPartyID>)
    }
    }
    </n1:requestCommon>
}
