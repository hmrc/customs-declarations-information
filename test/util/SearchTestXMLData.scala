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

package util

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, Month, ZoneId, ZonedDateTime}
import scala.xml.{Elem, Node, NodeSeq, XML}

object SearchTestXMLData {
  val defaultDateTime: ZonedDateTime = LocalDateTime.of(2020, Month.JUNE, 15, 12, 30, 0, 0).atZone(ZoneId.of("UTC"))
  //TODO investigate duplication of dates around the tests and test data in general. Should be consolidated in one place and not reused
  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss'Z'")

  val validNonCspSearchRequestPayload: String =
    """<n1:retrieveDeclarationSummaryDataRequest
      |          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      |          xmlns:n1="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest"
      |          xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest retrieveDeclarationSummaryDataRequest.xsd">
      |          <n1:requestCommon>
      |            <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
      |            <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
      |            <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
      |            <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
      |            <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
      |          </n1:requestCommon>
      |            <n1:requestDetail>
      |              <n1:eori>GB123456789000</n1:eori>
      |              <n1:partyRole>submitter</n1:partyRole>
      |              <n1:declarationCategory>IM</n1:declarationCategory>
      |              <n1:declarationStatus>all</n1:declarationStatus>
      |              <n1:goodsLocationCode>BELBELOB4</n1:goodsLocationCode>
      |              <n1:dateRange>
      |                <n1:dateFrom>2021-04-01</n1:dateFrom>
      |                <n1:dateTo>2021-04-04</n1:dateTo>
      |              </n1:dateRange>
      |              <n1:pageNumber>2</n1:pageNumber>
      |            </n1:requestDetail>
      |          </n1:retrieveDeclarationSummaryDataRequest>
      |""".stripMargin

  val validNonCspSearchRequestPayloadWithSubChannel: String =
    """<n1:retrieveDeclarationSummaryDataRequest
      |          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      |          xmlns:n1="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest"
      |          xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest retrieveDeclarationSummaryDataRequest.xsd">
      |          <n1:requestCommon>
      |            <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
      |            <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
      |            <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
      |            <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
      |            <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
      |          </n1:requestCommon>
      |            <n1:requestDetail>
      |              <n1:eori>GB123456789000</n1:eori>
      |              <n1:partyRole>submitter</n1:partyRole>
      |              <n1:declarationCategory>IM</n1:declarationCategory>
      |              <n1:declarationStatus>all</n1:declarationStatus>
      |              <n1:goodsLocationCode>BELBELOB4</n1:goodsLocationCode>
      |              <n1:dateRange>
      |                <n1:dateFrom>2021-04-01</n1:dateFrom>
      |                <n1:dateTo>2021-04-04</n1:dateTo>
      |              </n1:dateRange>
      |              <n1:pageNumber>2</n1:pageNumber>
      |              <n1:declarationSubmissionChannel>AuthenticatedPartyOnly</n1:declarationSubmissionChannel>
      |            </n1:requestDetail>
      |          </n1:retrieveDeclarationSummaryDataRequest>
      |""".stripMargin

  val validNonCspSearchRequestPayloadWithMandatoryParametersOnly: String =
    """<n1:retrieveDeclarationSummaryDataRequest
      |          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      |          xmlns:n1="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest"
      |          xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest retrieveDeclarationSummaryDataRequest.xsd">
      |          <n1:requestCommon>
      |            <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
      |            <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
      |            <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
      |            <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
      |            <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
      |          </n1:requestCommon>
      |            <n1:requestDetail>
      |              <n1:partyRole>submitter</n1:partyRole>
      |              <n1:declarationCategory>IM</n1:declarationCategory>
      |            </n1:requestDetail>
      |          </n1:retrieveDeclarationSummaryDataRequest>
      |""".stripMargin

  val validNonCspSearchRequestPayloadWithMandatoryParametersOnlyAndDate: String =
    """<n1:retrieveDeclarationSummaryDataRequest
      |          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      |          xmlns:n1="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest"
      |          xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest retrieveDeclarationSummaryDataRequest.xsd">
      |          <n1:requestCommon>
      |            <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
      |            <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
      |            <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
      |            <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
      |            <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
      |          </n1:requestCommon>
      |            <n1:requestDetail>
      |              <n1:partyRole>submitter</n1:partyRole>
      |              <n1:declarationCategory>IM</n1:declarationCategory>
      |              <n1:dateRange>
      |                <n1:dateFrom>2021-04-01</n1:dateFrom>
      |                <n1:dateTo>2021-04-04</n1:dateTo>
      |              </n1:dateRange>
      |            </n1:requestDetail>
      |          </n1:retrieveDeclarationSummaryDataRequest>
      |""".stripMargin

  val validCspSearchRequestPayload: String =
      """<n1:retrieveDeclarationSummaryDataRequest
        |          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        |          xmlns:n1="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest"
        |          xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest retrieveDeclarationSummaryDataRequest.xsd">
        |            <n1:requestCommon>
        |              <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
        |              <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
        |              <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
        |              <n1:badgeIdentifier>BADGEID123</n1:badgeIdentifier>
        |              <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
        |              <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
        |              <n1:originatingPartyID>ZZ123456789000</n1:originatingPartyID>
        |            </n1:requestCommon>
        |            <n1:requestDetail>
        |              <n1:eori>GB123456789000</n1:eori>
        |              <n1:partyRole>submitter</n1:partyRole>
        |              <n1:declarationCategory>IM</n1:declarationCategory>
        |              <n1:declarationStatus>all</n1:declarationStatus>
        |              <n1:goodsLocationCode>BELBELOB4</n1:goodsLocationCode>
        |              <n1:dateRange>
        |                <n1:dateFrom>2021-04-01</n1:dateFrom>
        |                <n1:dateTo>2021-04-04</n1:dateTo>
        |              </n1:dateRange>
        |              <n1:pageNumber>2</n1:pageNumber>
        |              <n1:declarationSubmissionChannel>AuthenticatedPartyOnly</n1:declarationSubmissionChannel>
        |            </n1:requestDetail>
        |          </n1:retrieveDeclarationSummaryDataRequest>
        |""".stripMargin

  val validCspSearchRequestPayloadWithoutBadge: String =
    """<n1:retrieveDeclarationSummaryDataRequest
      |          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      |          xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest retrieveDeclarationSummaryDataRequest.xsd"
      |          xmlns:n1="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest">
      |            <n1:requestCommon>
      |              <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
      |              <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
      |              <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
      |              <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
      |              <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
      |              <n1:originatingPartyID>ZZ123456789000</n1:originatingPartyID>
      |            </n1:requestCommon>
      |            <n1:requestDetail>
      |              <n1:eori>GB123456789000</n1:eori>
      |              <n1:partyRole>submitter</n1:partyRole>
      |              <n1:declarationCategory>IM</n1:declarationCategory>
      |              <n1:declarationStatus>all</n1:declarationStatus>
      |              <n1:goodsLocationCode>BELBELOB4</n1:goodsLocationCode>
      |              <n1:dateRange>
      |                <n1:dateFrom>2021-04-01</n1:dateFrom>
      |                <n1:dateTo>2021-04-04</n1:dateTo>
      |              </n1:dateRange>
      |              <n1:pageNumber>2</n1:pageNumber>
      |              <n1:declarationSubmissionChannel>AuthenticatedPartyOnly</n1:declarationSubmissionChannel>
      |            </n1:requestDetail>
      |          </n1:retrieveDeclarationSummaryDataRequest>
      |""".stripMargin
  
  val expectedSearchPayloadRequest: Elem = {
    <n1:retrieveDeclarationSummaryDataRequest
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:n1="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest"
    xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationSummaryDataRequest retrieveDeclarationSummaryDataRequest.xsd">
      <n1:requestCommon>
        <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
        <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
        <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
        <n1:badgeIdentifier>BADGEID123</n1:badgeIdentifier>
        <n1:dateTimeStamp>2018-09-11T10:28:54.128Z</n1:dateTimeStamp>
        <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
        <n1:originatingPartyID>ZZ123456789000</n1:originatingPartyID>
      </n1:requestCommon>
      <n1:requestDetail>
        <n1:eori>GB123456789000</n1:eori>
        <n1:partyRole>submitter</n1:partyRole>
        <n1:declarationCategory>IM</n1:declarationCategory>
        <n1:declarationStatus>all</n1:declarationStatus>
        <n1:goodsLocationCode>BELBELOB4</n1:goodsLocationCode>
        <n1:dateRange>
          <n1:dateFrom>2021-04-01</n1:dateFrom>
          <n1:dateTo>2021-04-04</n1:dateTo>
        </n1:dateRange>
        <n1:pageNumber>2</n1:pageNumber>
      </n1:requestDetail>
    </n1:retrieveDeclarationSummaryDataRequest>
  }

  val validBackendSearchResponse: NodeSeq = {
    <n1:retrieveDeclarationSummaryDataResponse
    xmlns:od="urn:wco:datamodel:WCO:DEC-DMS:2"
    xmlns:otnds="urn:wco:datamodel:WCO:Response_DS:DMS:2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ds="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
    xmlns:n1="http://gov.uk/customs/declarationInformationRetrieval/declarationSummary/v1"
    xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/declarationSummary/v1 retrieveDeclarationSummaryDataResponse.xsd">
      <n1:responseCommon>
        <n1:processingDate>2001-12-17T09:30:47Z</n1:processingDate>
      </n1:responseCommon>
      <n1:responseDetail>
        <n1:declarationSummary>
          <n1:DeclarationSummaryDataList>
            <n1:DeclarationSummaryData>
              <n1:Declaration>
                <n1:ID>18GB9JLC3CU1LFGVR2</n1:ID>
                <n1:ReceivedDateTime>
                  <n1:DateTimeString formatCode="304">20190702110757Z</n1:DateTimeString>
                </n1:ReceivedDateTime>
                <n1:ROE>6</n1:ROE>
                <n1:ICS>15</n1:ICS>
                <n1:LRN>20GBAKZ81EQJ2WXYZ</n1:LRN>
              </n1:Declaration>
              <od:Declaration>
                <od:FunctionCode>9</od:FunctionCode>
                <od:TypeCode>IM</od:TypeCode>
                <od:Submitter>
                  <od:ID>GB123456789012000</od:ID>
                </od:Submitter>
                <od:Declarant>
                  <od:ID>GB123456789012000</od:ID>
                </od:Declarant>
                <od:GoodsShipment>
                  <od:Consignee>
                    <od:ID>GB123456789012123</od:ID>
                  </od:Consignee>
                  <od:Consignment>
                    <od:GoodsLocation>
                      <od:ID>LIVLIVLIVR</od:ID>
                      <od:TypeCode>14</od:TypeCode>
                    </od:GoodsLocation>
                  </od:Consignment>
                  <od:Consignor>
                    <od:ID>GB123456789012987</od:ID>
                  </od:Consignor>
                  <od:PreviousDocument>
                    <od:ID>18GBAKZ81EQJ2FGVR</od:ID>
                    <od:TypeCode>DCR</od:TypeCode>
                  </od:PreviousDocument>
                  <od:PreviousDocument>
                    <od:ID>18GBAKZ81EQJ2FGVA</od:ID>
                    <od:TypeCode>MCR</od:TypeCode>
                  </od:PreviousDocument>
                </od:GoodsShipment>
              </od:Declaration>
            </n1:DeclarationSummaryData>
            <n1:CurrentPageNumber>1</n1:CurrentPageNumber>
            <n1:TotalResultsAvailable>1</n1:TotalResultsAvailable>
            <n1:TotalPagesAvailable>1</n1:TotalPagesAvailable>
            <n1:NoResultsReturned>false</n1:NoResultsReturned>
          </n1:DeclarationSummaryDataList>
        </n1:declarationSummary>
      </n1:responseDetail>
    </n1:retrieveDeclarationSummaryDataResponse>
  }.filter(_ => true)

  val validFilteredSearchResponseXML: Elem = {
    <p:DeclarationSearchResponse xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/declarationSummary/v1"
                                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                 xmlns:p4="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6"
                                 xmlns:p3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
                                 xmlns:p2="urn:wco:datamodel:WCO:DEC-DMS:2"
                                 xmlns:p1="urn:wco:datamodel:WCO:Response_DS:DMS:2"
                                 xmlns:p="http://gov.uk/customs/declarationInformationRetrieval/declarationSummary/v1">
      <p:DeclarationSearchDetails>
        <p:Declaration>
          <p:ID>18GB9JLC3CU1LFGVR2</p:ID>
          <p:ReceivedDateTime>
            <p:DateTimeString formatCode="304">20200715123000Z</p:DateTimeString>
          </p:ReceivedDateTime>
          <p:ROE>6</p:ROE>
          <p:ICS>15</p:ICS>
          <p:LRN>20GBAKZ81EQJ2WXYZ</p:LRN>
        </p:Declaration>
        <p2:Declaration>
          <p2:FunctionCode>9</p2:FunctionCode>
          <p2:TypeCode>IMZ</p2:TypeCode>
          <p2:GoodsShipment>
            <p2:Consignment>
              <p2:GoodsLocation>
                <p2:ID>LIVLIVLIVR</p2:ID>
                <p2:TypeCode>14</p2:TypeCode>
              </p2:GoodsLocation>
            </p2:Consignment>
            <p2:PreviousDocument>
              <p2:ID>18GBAKZ81EQJ2FGVR</p2:ID>
              <p2:TypeCode>DCR</p2:TypeCode>
            </p2:PreviousDocument>
            <p2:PreviousDocument>
              <p2:ID>18GBAKZ81EQJ2FGVA</p2:ID>
              <p2:TypeCode>DCS</p2:TypeCode>
            </p2:PreviousDocument>
          </p2:GoodsShipment>
        </p2:Declaration>
      </p:DeclarationSearchDetails>
      <p:CurrentPageNumber>1</p:CurrentPageNumber>
      <p:TotalResultsAvailable>1</p:TotalResultsAvailable>
      <p:TotalPagesAvailable>2</p:TotalPagesAvailable>
      <p:NoResultsReturned>false</p:NoResultsReturned>
    </p:DeclarationSearchResponse>
  }
  
  def generateDeclarationSearchResponse(noOfDeclarationSearchResults: Int = 1, receivedDate: ZonedDateTime): NodeSeq = {
    val items = noOfDeclarationSearchResults to 1 by -1
    //TODO try and refactor this
    val content = items.map(index => generateDeclarationSearchDetailsElement(generateHMRCDeclaration(receivedDate.plusMonths(index)), generateStandardResponseWCODeclaration()))

    generateRootElements(content, noOfDeclarationSearchResults)
  }

  private def generateRootElements(content: Seq[NodeSeq], noOfDeclarationSearchResults: Int = 1): Elem =
    <n1:retrieveDeclarationSummaryDataResponse
    xmlns:od="urn:wco:datamodel:WCO:DEC-DMS:2"
    xmlns:otnds="urn:wco:datamodel:WCO:Response_DS:DMS:2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ds="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
    xmlns:n1="http://gov.uk/customs/declarationInformationRetrieval/declarationSummary/v1"
    xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/declarationSummary/v1 retrieveDeclarationSummaryDataResponse.xsd">
      <n1:responseCommon>
        <n1:processingDate>2001-12-17T09:30:47Z</n1:processingDate>
      </n1:responseCommon>
      <n1:responseDetail>
        <n1:declarationSummary>
          <n1:DeclarationSummaryDataList>
            {content}
            <n1:CurrentPageNumber>1</n1:CurrentPageNumber>
            <n1:TotalResultsAvailable>{noOfDeclarationSearchResults}</n1:TotalResultsAvailable>
            <n1:TotalPagesAvailable>2</n1:TotalPagesAvailable>
            <n1:NoResultsReturned>false</n1:NoResultsReturned>
          </n1:DeclarationSummaryDataList>
        </n1:declarationSummary>
      </n1:responseDetail>
    </n1:retrieveDeclarationSummaryDataResponse>

  private def generateDeclarationSearchDetailsElement(hmrcDeclaration: Node, wcoDeclaration: Node): NodeSeq =
    <n1:DeclarationSummaryData>
      {hmrcDeclaration}
      {wcoDeclaration}
    </n1:DeclarationSummaryData>


  private def generateHMRCDeclaration(receivedDate: ZonedDateTime): Elem =
    <n1:Declaration>
      <n1:ID>18GB9JLC3CU1LFGVR2</n1:ID>
      <n1:ReceivedDateTime>
        <n1:DateTimeString formatCode="304">{receivedDate.format(dateTimeFormatter)}</n1:DateTimeString>
      </n1:ReceivedDateTime>
      <n1:ROE>6</n1:ROE>
      <n1:ICS>15</n1:ICS>
      <n1:LRN>20GBAKZ81EQJ2WXYZ</n1:LRN>
    </n1:Declaration>
  
  private def generateStandardResponseWCODeclaration(): Elem =
    <od:Declaration>
      <od:FunctionCode>9</od:FunctionCode>
      <od:TypeCode>IMZ</od:TypeCode>
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

  def generateDeclarationResponseContainingAllOptionalElements(receivedDate: ZonedDateTime): NodeSeq = {
    val wcoDeclarationWithAllElementsPopulated: Node = XML.loadFile("test/resources/xml/sample_wco_dec_containing_all_possible_elements.xml").head
    val hmrcDeclaration: Elem = generateHMRCDeclaration(receivedDate)
    val declarationSearchDetailsElement: NodeSeq = generateDeclarationSearchDetailsElement(hmrcDeclaration, wcoDeclarationWithAllElementsPopulated)

    generateRootElements(declarationSearchDetailsElement)
  }
}
