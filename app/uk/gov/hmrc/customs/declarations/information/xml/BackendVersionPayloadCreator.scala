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

package uk.gov.hmrc.customs.declarations.information.xml

import org.joda.time.DateTime
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest

import javax.inject.Singleton
import scala.xml.{NodeSeq, Text}

@Singleton
class BackendVersionPayloadCreator() extends BackendPayloadCreator {

  private val newLineAndIndentation = "\n        "

  override def create[A](conversationId: ConversationId,
                         correlationId: CorrelationId,
                         date: DateTime,
                         eitherMrnOrSearchType: Either[SearchType, Mrn],
                         maybeApiSubscriptionFieldsResponse: Option[ApiSubscriptionFieldsResponse])
  (implicit asr: AuthorisedRequest[A]): NodeSeq = {
    <ns1:retrieveDeclarationVersionRequest
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ns1="http://gov.uk/customs/retrieveDeclarationVersion"
    xsi:schemaLocation=" http://gov.uk/customs/retrieveDeclarationVersion retrieveDeclarationVersionRequest.xsd">
      <ns1:requestCommon>
        {Seq[NodeSeq](
        <ns1:clientID>99999999-9999-9999-9999-999999999999</ns1:clientID>, Text(newLineAndIndentation),
        <ns1:conversationID>{conversationId.toString}</ns1:conversationID>, Text(newLineAndIndentation),
        <ns1:correlationID>{correlationId.toString}</ns1:correlationID>)}
        {val as = asr.authorisedAs
        as match {
          case NonCsp(eori) => Seq[NodeSeq](
            <ns1:dateTimeStamp>{date.toString()}</ns1:dateTimeStamp>, Text(newLineAndIndentation),
            <ns1:authenticatedPartyID>{eori.toString}</ns1:authenticatedPartyID>)

          case Csp(_, badgeId) =>
            val badgeIdentifierElement: NodeSeq = {badgeId.fold(NodeSeq.Empty)(badge => <ns1:badgeIdentifier>{badge.toString}</ns1:badgeIdentifier>)}
            Seq[NodeSeq](badgeIdentifierElement, Text(newLineAndIndentation),
              <ns1:dateTimeStamp>{date.toString}</ns1:dateTimeStamp>, Text(newLineAndIndentation),
              <ns1:authenticatedPartyID>{maybeApiSubscriptionFieldsResponse.get.fields.authenticatedEori.get}</ns1:authenticatedPartyID>, Text(newLineAndIndentation),
              <ns1:originatingPartyID>{Csp.originatingPartyId(as.asInstanceOf[Csp])}</ns1:originatingPartyID>)
        }

        }
      </ns1:requestCommon>
      <ns1:requestDetail>
        <ns1:RetrieveDeclarationVersionRequest>
          <ns1:ServiceRequestParameters>
            <ns1:MRN>{eitherMrnOrSearchType.right.get}</ns1:MRN>
            {asr.declarationSubmissionChannel.fold(NodeSeq.Empty)(apo => <ns1:DeclarationSubmissionChannel>{apo}</ns1:DeclarationSubmissionChannel>)}
          </ns1:ServiceRequestParameters>
        </ns1:RetrieveDeclarationVersionRequest>
      </ns1:requestDetail>
    </ns1:retrieveDeclarationVersionRequest>
  }


}
