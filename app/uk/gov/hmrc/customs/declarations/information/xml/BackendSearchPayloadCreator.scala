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

//TODO this is (mostly) a copy of version payload creator for now. Some of the element names are correct.
@Singleton
class BackendSearchPayloadCreator() extends BackendPayloadCreator {

  private val newLineAndIndentation = "\n        "

  override def create[A](conversationId: ConversationId,
                         correlationId: CorrelationId,
                         date: DateTime,
                         searchType: SearchType,
                         maybeApiSubscriptionFieldsResponse: Option[ApiSubscriptionFieldsResponse])
  (implicit asr: AuthorisedRequest[A]): NodeSeq = {
    <ns1:retrieveDeclarationSummaryDataRequest
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ns1="http://gov.uk/customs/retrieveDeclarationVersion"
    xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest retrieveDeclarationVersionRequest.xsd">
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
      <ns1:requestDetail> <!-- no wrapper elements - children here are list of parameters -->
        <ns1:partyRole>submitter</ns1:partyRole>
        <!-- //TODO elements below require setting according to values passed in  -->
        <ns1:declarationCategory>IM</ns1:declarationCategory>
        <ns1:declarationStatus>cleared</ns1:declarationStatus>
        <ns1:goodsLocationCode>BELBELOB4</ns1:goodsLocationCode>
        <ns1:dateRange>
          <ns1:dateFrom>2021-05-19</ns1:dateFrom>
          <ns1:dateTo>2021-06-19</ns1:dateTo>
        </ns1:dateRange>
        <ns1:pageNumber>3</ns1:pageNumber>
        {asr.declarationSubmissionChannel.fold(NodeSeq.Empty)(apo => <ns1:DeclarationSubmissionChannel>{apo}</ns1:DeclarationSubmissionChannel>)}
      </ns1:requestDetail>
    </ns1:retrieveDeclarationSummaryDataRequest>
  }


}
