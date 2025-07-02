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

package uk.gov.hmrc.customs.declarations.information.xml

import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.util.DateTimeUtils

import java.time.ZonedDateTime
import javax.inject.Singleton
import scala.xml.NodeSeq

@Singleton
class BackendSearchPayloadCreator() extends BackendPayloadCreator {
  override def create[A](conversationId: ConversationId,
                         correlationId: CorrelationId,
                         date: ZonedDateTime,
                         searchType: SearchType,
                         maybeApiSubscriptionFieldsResponse: Option[ApiSubscriptionFieldsResponse])(implicit asr: AuthorisedRequest[A]): NodeSeq = {
    val searchParameters = asr.searchParameters.get

    <n1:retrieveDeclarationSummaryDataRequest
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:n1="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest"
    xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest retrieveDeclarationSummaryDataRequest.xsd">
       {requestCommon(conversationId, correlationId, date, searchType, maybeApiSubscriptionFieldsResponse)}
      <n1:requestDetail>
        {searchParameters.eori.fold(NodeSeq.Empty)((e) => <n1:eori>{e}</n1:eori>)}
        <n1:partyRole>{searchParameters.partyRole.toString}</n1:partyRole>
        <n1:declarationCategory>{searchParameters.declarationCategory.toString}</n1:declarationCategory>
        {searchParameters.declarationStatus.fold(NodeSeq.Empty)(ds => <n1:declarationStatus>{ds}</n1:declarationStatus>)}
        {searchParameters.goodsLocationCode.fold(NodeSeq.Empty)(glc => <n1:goodsLocationCode>{glc}</n1:goodsLocationCode>)}
        {
        if (searchParameters.dateFrom.isDefined || searchParameters.dateTo.isDefined) {
          <n1:dateRange>
            {searchParameters.dateFrom.map(df => <n1:dateFrom>{DateTimeUtils.dateFormat.format(df)}</n1:dateFrom>).getOrElse(NodeSeq.Empty)}
            {searchParameters.dateTo.map(dt => <n1:dateTo>{DateTimeUtils.dateFormat.format(dt)}</n1:dateTo>).getOrElse(NodeSeq.Empty)}
          </n1:dateRange>
        } else NodeSeq.Empty
        }
        {searchParameters.pageNumber.fold(NodeSeq.Empty)(pn => <n1:pageNumber>{pn}</n1:pageNumber>)}
        {asr.declarationSubmissionChannel.fold(NodeSeq.Empty)(apo => <n1:declarationSubmissionChannel>{apo}</n1:declarationSubmissionChannel>)}
      </n1:requestDetail>
    </n1:retrieveDeclarationSummaryDataRequest>
  }
}
