/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.Singleton
import org.joda.time.DateTime
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.customs.declarations.information.model._

import scala.xml.{NodeSeq, Text}

@Singleton
class MdgPayloadCreator() {

  private val newLineAndIndentation = "\n        "

  def create[A](correlationId: CorrelationId,
                date: DateTime,
                mrn: Mrn,
                apiSubscriptionFieldsResponse: ApiSubscriptionFieldsResponse)
               (implicit asr: AuthorisedRequest[A]): NodeSeq = {
    <n1:queryDeclarationInformationRequest
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd_1="http://trade.core.ecf/messages/2017/03/31/"
    xmlns:n1="http://gov.uk/customs/retrieveDeclarationInformation/v1" xmlns:tns_1="http://cmm.core.ecf/BaseTypes/cmmServiceTypes/trade/2017/02/22/"
    xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationInformation/v1 request_schema.xsd">
      <n1:requestCommon>
        <n1:clientID>{apiSubscriptionFieldsResponse.fieldsId.toString}</n1:clientID>
        <n1:conversationID>{asr.conversationId.toString}</n1:conversationID>
        <n1:correlationID>{correlationId.toString}</n1:correlationID>
        {val as = asr.authorisedAs

      as match {
        case NonCsp(eori) =>
          <v1:authenticatedPartyID>{eori.value}</v1:authenticatedPartyID> // originatingPartyID is only required for CSPs
        case Csp(_, badgeId) =>
          val badgeIdentifierElement: NodeSeq = {badgeId.fold(NodeSeq.Empty)(badge => <v1:badgeIdentifier>{badge.toString}</v1:badgeIdentifier>)}
          Seq[NodeSeq](
            badgeIdentifierElement, Text(newLineAndIndentation),
            <v1:originatingPartyID>{Csp.originatingPartyId(as.asInstanceOf[Csp])}</v1:originatingPartyID>, Text(newLineAndIndentation),
            <v1:authenticatedPartyID>{apiSubscriptionFieldsResponse.fields.authenticatedEori.get}</v1:authenticatedPartyID>)
      }
        }
        <n1:dateTimeStamp>{date.toString}</n1:dateTimeStamp>
      </n1:requestCommon>
      <n1:requestDetail>
        <n1:declarationManagementInformationRequest>
          <tns_1:timeStamp>{date.toString}</tns_1:timeStamp>
          <xsd_1:reference>{mrn.toString}</xsd_1:reference>
        </n1:declarationManagementInformationRequest>
      </n1:requestDetail>
    </n1:queryDeclarationInformationRequest>
  }
}
