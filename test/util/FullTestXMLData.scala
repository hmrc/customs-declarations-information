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

package util

import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.{DateTime, DateTimeFieldType, DateTimeZone}

import scala.xml.Elem

object FullTestXMLData {

  val defaultDateTime = DateTime.now(DateTimeZone.UTC)
    .withYear(2020)
    .withMonthOfYear(6)
    .withDayOfMonth(15)
    .withHourOfDay(12)
    .withMinuteOfHour(30)
    .withSecondOfMinute(0)
    .withMillisOfSecond(0)

  val dateTimeFormat = new DateTimeFormatterBuilder()
    .appendYear(4, 4)
    .appendFixedDecimal(DateTimeFieldType.monthOfYear(), 2)
    .appendFixedDecimal(DateTimeFieldType.dayOfMonth(), 2)
    .appendFixedDecimal(DateTimeFieldType.hourOfDay, 2)
    .appendFixedDecimal(DateTimeFieldType.minuteOfHour, 2)
    .appendFixedDecimal(DateTimeFieldType.secondOfMinute, 2)
    .appendTimeZoneOffset("Z", false, 2, 2)
    .toFormatter

  val validNonCspFullRequestPayload =
    """<n1:getFullDeclarationDataRequest xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd"
      |xmlns:n1="http://gov.uk/customs/FullDeclarationDataRetrievalService" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      |          <n1:requestCommon>
      |            <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
      |            <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
      |            <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
      |            <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
      |            <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
      |          </n1:requestCommon>
      |          <n1:requestDetail>
      |            <n1:FullDeclarationDataRetrievalRequest>
      |              <n1:ServiceRequestParameters>
      |                <n1:MRN>theMrn</n1:MRN>
      |                <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>
      |              </n1:ServiceRequestParameters>
      |            </n1:FullDeclarationDataRetrievalRequest>
      |          </n1:requestDetail>
      |        </n1:getFullDeclarationDataRequest>
      |""".stripMargin

  val validNonCspFullRequestPayloadWithDeclarationSubmissionChannel =
    """<n1:getFullDeclarationDataRequest xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd"
      |xmlns:n1="http://gov.uk/customs/FullDeclarationDataRetrievalService" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      |          <n1:requestCommon>
      |            <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
      |            <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
      |            <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
      |            <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
      |            <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
      |          </n1:requestCommon>
      |          <n1:requestDetail>
      |            <n1:FullDeclarationDataRetrievalRequest>
      |              <n1:ServiceRequestParameters>
      |                <n1:MRN>theMrn</n1:MRN>
      |                <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>
      |                <n1:DeclarationSubmissionChannel>AuthenticatedPartyOnly</n1:DeclarationSubmissionChannel>
      |              </n1:ServiceRequestParameters>
      |            </n1:FullDeclarationDataRetrievalRequest>
      |          </n1:requestDetail>
      |        </n1:getFullDeclarationDataRequest>
      |""".stripMargin

  val validCspFullRequestPayload =
    """<n1:getFullDeclarationDataRequest xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd"
      |xmlns:n1="http://gov.uk/customs/FullDeclarationDataRetrievalService" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      |          <n1:requestCommon>
      |            <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
      |            <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
      |            <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
      |            <n1:badgeIdentifier>BADGEID123</n1:badgeIdentifier>
      |            <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
      |            <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
      |            <n1:originatingPartyID>ZZ123456789000</n1:originatingPartyID>
      |          </n1:requestCommon>
      |          <n1:requestDetail>
      |            <n1:FullDeclarationDataRetrievalRequest>
      |              <n1:ServiceRequestParameters>
      |                <n1:MRN>theMrn</n1:MRN>
      |                <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>
      |              </n1:ServiceRequestParameters>
      |            </n1:FullDeclarationDataRetrievalRequest>
      |          </n1:requestDetail>
      |        </n1:getFullDeclarationDataRequest>
      |""".stripMargin

  val validCspFullRequestPayloadWithDeclarationSubmissionChannel =
    """<n1:getFullDeclarationDataRequest xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd"
      |xmlns:n1="http://gov.uk/customs/FullDeclarationDataRetrievalService" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      |          <n1:requestCommon>
      |            <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
      |            <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
      |            <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
      |            <n1:badgeIdentifier>BADGEID123</n1:badgeIdentifier>
      |            <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
      |            <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
      |            <n1:originatingPartyID>ZZ123456789000</n1:originatingPartyID>
      |          </n1:requestCommon>
      |          <n1:requestDetail>
      |            <n1:FullDeclarationDataRetrievalRequest>
      |              <n1:ServiceRequestParameters>
      |                <n1:MRN>theMrn</n1:MRN>
      |                <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>
      |                <n1:DeclarationSubmissionChannel>AuthenticatedPartyOnly</n1:DeclarationSubmissionChannel>
      |              </n1:ServiceRequestParameters>
      |            </n1:FullDeclarationDataRetrievalRequest>
      |          </n1:requestDetail>
      |        </n1:getFullDeclarationDataRequest>
      |""".stripMargin

  val validCspFullRequestWithoutBadgePayload =
    """<n1:getFullDeclarationDataRequest xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd"
      |xmlns:n1="http://gov.uk/customs/FullDeclarationDataRetrievalService" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      |          <n1:requestCommon>
      |            <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
      |            <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
      |            <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
      |            <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
      |            <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
      |            <n1:originatingPartyID>ZZ123456789000</n1:originatingPartyID>
      |          </n1:requestCommon>
      |          <n1:requestDetail>
      |            <n1:FullDeclarationDataRetrievalRequest>
      |              <n1:ServiceRequestParameters>
      |                <n1:MRN>theMrn</n1:MRN>
      |                <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>
      |              </n1:ServiceRequestParameters>
      |            </n1:FullDeclarationDataRetrievalRequest>
      |          </n1:requestDetail>
      |        </n1:getFullDeclarationDataRequest>
      |""".stripMargin

  val expectedFullPayloadRequest = {
    <n1:getFullDeclarationDataRequest
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:n1="http://gov.uk/customs/FullDeclarationDataRetrievalService"
    xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd">
      <n1:requestCommon>
        <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
        <n1:conversationID>5d2ab59f-da76-446d-b645-38e6fdfa2e98</n1:conversationID>
        <n1:correlationID>c7422214-ed73-48ab-b03b-e709275af5ad</n1:correlationID>
        <n1:badgeIdentifier>BADGEID123</n1:badgeIdentifier>
        <n1:dateTimeStamp>2021-05-17T09:30:47Z</n1:dateTimeStamp>
        <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
        <n1:originatingPartyID>BADGEID123</n1:originatingPartyID>
      </n1:requestCommon>
      <n1:requestDetail>
        <n1:FullDeclarationDataRetrievalRequest>
          <n1:ServiceRequestParameters>
            <n1:MRN>someMrn</n1:MRN>
            <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>
            <n1:DeclarationSubmissionChannel>AuthenticatedPartyOnly</n1:DeclarationSubmissionChannel>
          </n1:ServiceRequestParameters>
        </n1:FullDeclarationDataRetrievalRequest>
      </n1:requestDetail>
    </n1:getFullDeclarationDataRequest>
  }

  val validBackendFullResponse = {
    <n1:retrieveDeclarationVersionResponse xmlns:od="urn:wco:datamodel:WCO:DEC-DMS:2"
                                           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                           xmlns:Q1="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
                                           xmlns:n1="http://gov.uk/customs/retrieveDeclarationVersion"
                                           xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationVersion retrieveDeclarationVersionResponse.xsd">
      <n1:responseCommon>
        <n1:processingDate>2001-12-17T09:30:47Z</n1:processingDate>
      </n1:responseCommon>
      <n1:responseDetail>
        <n1:RetrieveDeclarationVersionResponse>
          <n1:RetrieveDeclarationVersionDetailsList>
            <n1:RetrieveDeclarationVersionDetails>
              <n1:Declaration>
                <n1:ID>18GB9JLC3CU1LFGVR2</n1:ID>
                <n1:VersionID>2</n1:VersionID>
                <n1:CreatedDateTime>
                  <n1:DateTimeString formatCode="304">20190702110757Z</n1:DateTimeString>
                </n1:CreatedDateTime>
                <n1:LRN>20GBAKZ81EQJ2WXYZ</n1:LRN>
              </n1:Declaration>
              <od:Declaration>
                <od:FunctionCode>9</od:FunctionCode>
                <od:TypeCode>IM</od:TypeCode>
                <od:GoodsShipment>
                  <od:Consignment>
                    <od:GoodsLocation>
                      <od:ID>LIVLIVLIVR</od:ID>
                      <od:TypeCode>14</od:TypeCode>
                    </od:GoodsLocation>
                  </od:Consignment>
                  <od:PreviousDocument>
                    <od:ID>18GBAKZ81EQJ2FGVR</od:ID>
                    <od:TypeCode>DCR</od:TypeCode>
                  </od:PreviousDocument>
                  <od:PreviousDocument>
                    <od:ID>18GBAKZ81EQJ2FGVA</od:ID>
                    <od:TypeCode>DCS</od:TypeCode>
                  </od:PreviousDocument>
                </od:GoodsShipment>
              </od:Declaration>
            </n1:RetrieveDeclarationVersionDetails>
            <n1:RetrieveDeclarationVersionDetails>
              <n1:Declaration>
                <n1:ID>18GB9JLC3CU1LFGVR3</n1:ID>
                <n1:VersionID>1</n1:VersionID>
                <n1:CreatedDateTime>
                  <n1:DateTimeString formatCode="304">20190703120957Z</n1:DateTimeString>
                </n1:CreatedDateTime>
                <n1:LRN>30GBAKZ81EQJ2WXYZ</n1:LRN>
              </n1:Declaration>
              <od:Declaration>
                <od:FunctionCode>9</od:FunctionCode>
                <od:TypeCode>IM</od:TypeCode>
                <od:GoodsShipment>
                  <od:Consignment>
                    <od:GoodsLocation>
                      <od:ID>LIVLIVLIVR</od:ID>
                      <od:TypeCode>15</od:TypeCode>
                    </od:GoodsLocation>
                  </od:Consignment>
                  <od:PreviousDocument>
                    <od:ID>18GBAKZ81EQJ2FGVX</od:ID>
                    <od:TypeCode>DCR</od:TypeCode>
                  </od:PreviousDocument>
                  <od:PreviousDocument>
                    <od:ID>18GBAKZ81EQJ2FGVZ</od:ID>
                    <od:TypeCode>DCS</od:TypeCode>
                  </od:PreviousDocument>
                </od:GoodsShipment>
              </od:Declaration>
            </n1:RetrieveDeclarationVersionDetails>
          </n1:RetrieveDeclarationVersionDetailsList>
        </n1:RetrieveDeclarationVersionResponse>
      </n1:responseDetail>
    </n1:retrieveDeclarationVersionResponse>
  }.filter(_ => true)

  val backendDeclarationFullResponse: Elem =
    <n1:fullDeclarationDataResponse
    xmlns:Q1="urn:wco:datamodel:WCO:Response_DS:DMS:2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ds="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
    xmlns:n1="http://gov.uk/customs/FullDeclarationDataRetrievalService"
    xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataResponse.xsd">
      <n1:responseCommon>
        <n1:processingDate>2001-12-17T09:30:47Z</n1:processingDate>
      </n1:responseCommon>
      <n1:responseDetail>
        <n1:FullDeclarationDataRetrievalResponse>
          <n1:FullDeclarationDataDetails>
            <n1:HighLevelSummaryDetails>
              <n1:MRN>18GB9JLC3CU1LFGVR2</n1:MRN>
              <n1:LRN>20GBAKZ81EQJ2WXYZ</n1:LRN>
              <n1:DUCRandPartID>0GB123456789123-12345-1</n1:DUCRandPartID>
              <n1:VersionID>4</n1:VersionID>
              <n1:GoodsLocationCode>GBBUEDIEDIEDI</n1:GoodsLocationCode>
              <n1:CreatedDateTime>
                <n1:DateTimeString formatCode="304">20190702110757Z</n1:DateTimeString>
              </n1:CreatedDateTime>
              <n1:AcceptanceDateTime>
                <Q1:DateTimeString formatCode="304">20190702110757Z</Q1:DateTimeString>
              </n1:AcceptanceDateTime>
              <n1:FinalisedDateTime>
                <n1:DateTimeString formatCode="304">20190702110757Z</n1:DateTimeString>
              </n1:FinalisedDateTime>
            </n1:HighLevelSummaryDetails>
            <n1:GeneratedConsigmentDetails>
              <n1:ROE>6</n1:ROE>
              <n1:InventoryReference>02223305</n1:InventoryReference>
              <n1:StatusOfEntry-ICS>3</n1:StatusOfEntry-ICS>
              <n1:GoodsArrivalDateTime>
                <n1:DateTimeString formatCode="304">20190702110757Z</n1:DateTimeString>
              </n1:GoodsArrivalDateTime>
              <n1:DeclaredCustomsExitOffice>GB000072</n1:DeclaredCustomsExitOffice>
              <n1:ActualCustomsExitOffice>GB000290</n1:ActualCustomsExitOffice>
              <n1:SubmitterID>GB025165101000</n1:SubmitterID>
              <n1:StatisticalValue currencyID="GBP">1000.00</n1:StatisticalValue>
              <n1:ExitResult>A1</n1:ExitResult>
              <n1:DateOfExit>
                <n1:DateTimeString formatCode="304">20190702110757Z</n1:DateTimeString>
              </n1:DateOfExit>
            </n1:GeneratedConsigmentDetails>
            <n1:FullDeclarationObject>
              <n1:Declaration>
                <n1:AcceptanceDateTime>
                  <ds:DateTimeString formatCode="304">20000000</ds:DateTimeString>
                </n1:AcceptanceDateTime>
                <n1:FunctionCode>9</n1:FunctionCode>
                <n1:FunctionalReferenceID>eamonn2217</n1:FunctionalReferenceID>
                <n1:TypeCode>EX</n1:TypeCode>
                <n1:ProcedureCategory>H1</n1:ProcedureCategory>
                <n1:GoodsItemQuantity>1</n1:GoodsItemQuantity>
              </n1:Declaration>
            </n1:FullDeclarationObject>
          </n1:FullDeclarationDataDetails>
        </n1:FullDeclarationDataRetrievalResponse>
      </n1:responseDetail>
    </n1:fullDeclarationDataResponse>


  val validFilteredFullResponseXML: Elem = {
    <p:DeclarationFullResponse xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService"
                               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                               xmlns:p4="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6"
                               xmlns:p3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
                               xmlns:p2="urn:wco:datamodel:WCO:DEC-DMS:2"
                               xmlns:p1="urn:wco:datamodel:WCO:Response_DS:DMS:2"
                               xmlns:p="http://gov.uk/customs/FullDeclarationDataRetrievalService">
      <p:FullDeclarationDataDetails>
        <p:HighLevelSummaryDetails>
          <p:MRN>18GB9JLC3CU1LFGVR2</p:MRN>
          <p:LRN>20GBAKZ81EQJ2WXYZ</p:LRN>
          <p:DUCRandPartID>0GB123456789123-12345-1</p:DUCRandPartID>
          <p:VersionID>4</p:VersionID>
          <p:GoodsLocationCode>GBBUEDIEDIEDI</p:GoodsLocationCode>
          <p:CreatedDateTime>
            <p:DateTimeString formatCode="304">20190702110757Z</p:DateTimeString>
          </p:CreatedDateTime>
          <p:AcceptanceDateTime>
            <p1:DateTimeString formatCode="304">20190702110757Z</p1:DateTimeString>
          </p:AcceptanceDateTime>
          <p:FinalisedDateTime>
            <p:DateTimeString formatCode="304">20190702110757Z</p:DateTimeString>
          </p:FinalisedDateTime>
        </p:HighLevelSummaryDetails>
        <p:GeneratedConsigmentDetails>
          <p:ROE>6</p:ROE>
          <p:InventoryReference>02223305</p:InventoryReference>
          <p:StatusOfEntry-ICS>3</p:StatusOfEntry-ICS>
          <p:GoodsArrivalDateTime>
            <p:DateTimeString formatCode="304">20190702110757Z</p:DateTimeString>
          </p:GoodsArrivalDateTime>
          <p:DeclaredCustomsExitOffice>GB000072</p:DeclaredCustomsExitOffice>
          <p:ActualCustomsExitOffice>GB000290</p:ActualCustomsExitOffice>
          <p:SubmitterID>GB025165101000</p:SubmitterID>
          <p:StatisticalValue currencyID="GBP">1000.00</p:StatisticalValue>
          <p:ExitResult>A1</p:ExitResult>
          <p:DateOfExit>
            <p:DateTimeString formatCode="304">20190702110757Z</p:DateTimeString>
          </p:DateOfExit>
        </p:GeneratedConsigmentDetails>
        <p:FullDeclarationObject>
          <p:Declaration>
            <p:AcceptanceDateTime>
              <p3:DateTimeString formatCode="304">20000000</p3:DateTimeString>
            </p:AcceptanceDateTime>
            <p:FunctionCode>9</p:FunctionCode>
            <p:FunctionalReferenceID>eamonn2217</p:FunctionalReferenceID>
            <p:TypeCode>EX</p:TypeCode>
            <p:ProcedureCategory>H1</p:ProcedureCategory>
            <p:GoodsItemQuantity>1</p:GoodsItemQuantity>
          </p:Declaration>
        </p:FullDeclarationObject>
      </p:FullDeclarationDataDetails>
    </p:DeclarationFullResponse>
  }
}
