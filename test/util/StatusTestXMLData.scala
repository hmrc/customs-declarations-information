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

import scala.xml.{Elem, Node, NodeSeq, XML}

object StatusTestXMLData {

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

  val expectedStatusPayloadRequest: Elem =
    <n1:queryDeclarationStatusRequest
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:n1="http://gov.uk/customs/declarationInformationRetrieval/status/v2"
    xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v2 queryDeclarationStatusRequest.xsd">
      <n1:requestCommon>
        <n1:clientID>327d9145-4965-4d28-a2c5-39dedee50334</n1:clientID>
        <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
        <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
        <n1:badgeIdentifier>BADGEID123</n1:badgeIdentifier>
        <n1:dateTimeStamp>2018-09-11T10:28:54.128Z</n1:dateTimeStamp>
        <v1:authenticatedPartyID>ZZ123456789000</v1:authenticatedPartyID>
        <v1:originatingPartyID>BADGEID123</v1:originatingPartyID>
      </n1:requestCommon>
      <n1:requestDetail>
        <n1:retrieveDeclarationStatusRequest>
          <n1:MRN>theMRN</n1:MRN>
        </n1:retrieveDeclarationStatusRequest>
      </n1:requestDetail>
    </n1:queryDeclarationStatusRequest>

  val validBackendStatusResponse = {
    <n1:queryDeclarationStatusResponse
    xmlns:od="urn:wco:datamodel:WCO:DEC-DMS:2"
    xmlns:otnds="urn:wco:datamodel:WCO:Response_DS:DMS:2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ds="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
    xmlns:n1="http://gov.uk/customs/declarationInformationRetrieval/status/v2"
    xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v2 queryDeclarationStatusResponse.xsd">
      <n1:responseCommon>
        <n1:processingDate>2001-12-17T09:30:47Z</n1:processingDate>
      </n1:responseCommon>
      <n1:responseDetail>
        <n1:retrieveDeclarationStatusResponse>
          <n1:retrieveDeclarationStatusDetailsList>
            <n1:retrieveDeclarationStatusDetails>
              <n1:Declaration>
                <n1:AcceptanceDateTime>
                  <otnds:DateTimeString formatCode="304">20190702110757Z</otnds:DateTimeString>
                </n1:AcceptanceDateTime>
                <n1:ID>18GB9JLC3CU1LFGVR2</n1:ID>
                <n1:VersionID>1</n1:VersionID>
                <n1:ReceivedDateTime>
                  <n1:DateTimeString formatCode="304">20190702110757Z</n1:DateTimeString>
                </n1:ReceivedDateTime>
                <n1:GoodsReleasedDateTime>
                  <n1:DateTimeString formatCode="304">20190702110757Z</n1:DateTimeString>
                </n1:GoodsReleasedDateTime>
                <n1:ROE>6</n1:ROE>
                <n1:ICS>15</n1:ICS>
                <n1:IRC>000</n1:IRC>
              </n1:Declaration>
              <od:Declaration>
                <od:FunctionCode>9</od:FunctionCode>
                <od:TypeCode>IMZ</od:TypeCode>
                <od:GoodsItemQuantity>100</od:GoodsItemQuantity>
                <od:TotalPackageQuantity>10</od:TotalPackageQuantity>
                <od:Submitter>
                  <od:ID>GB123456789012000</od:ID>
                </od:Submitter>
                <od:GoodsShipment>
                  <od:PreviousDocument>
                    <od:ID>18GBAKZ81EQJ2FGVR</od:ID>
                    <od:TypeCode>DCR</od:TypeCode>
                  </od:PreviousDocument>
                  <od:PreviousDocument>
                    <od:ID>18GBAKZ81EQJ2FGVA</od:ID>
                    <od:TypeCode>MCR</od:TypeCode>
                  </od:PreviousDocument>
                  <od:UCR>
                    <od:TraderAssignedReferenceID>20GBAKZ81EQJ2WXYZ</od:TraderAssignedReferenceID>
                  </od:UCR>
                </od:GoodsShipment>
              </od:Declaration>
            </n1:retrieveDeclarationStatusDetails>
          </n1:retrieveDeclarationStatusDetailsList>
        </n1:retrieveDeclarationStatusResponse>
      </n1:responseDetail>
    </n1:queryDeclarationStatusResponse>
    }.filter(_ => true)

  val actualBackendStatusResponse = {
    <v2:queryDeclarationStatusResponse xmlns:v2="http://gov.uk/customs/declarationInformationRetrieval/status/v2" xmlns:urn="urn:wco:datamodel:WCO:Response_DS:DMS:2" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:dec="http://dmirs.core.ecf/DeclarationInformationRetrieval" xmlns:urn1="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:urn2="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:xs="http://www.w3.org/2001/XMLSchema">
      <v2:responseCommon>
        <v2:processingDate>2020-02-19T12:08:12.952Z</v2:processingDate>
      </v2:responseCommon>
      <v2:responseDetail>
        <v2:retrieveDeclarationStatusResponse>
          <v2:retrieveDeclarationStatusDetailsList>
            <v2:retrieveDeclarationStatusDetails>
              <ns3:Declaration xmlns:ns3="http://gov.uk/customs/declarationInformationRetrieval/status/v2" xmlns="http://dmirs.core.ecf/DeclarationInformationRetrieval" xmlns:ns5="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:ns2="urn:wco:datamodel:WCO:Response_DS:DMS:2" xmlns:ns4="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
                <ns3:AcceptanceDateTime>
                  <ns2:DateTimeString formatCode="304">20191010000000Z</ns2:DateTimeString>
                </ns3:AcceptanceDateTime>
                <ns3:ID>20GB1YQEOT8BCFGVR3</ns3:ID>
                <ns3:VersionID>1</ns3:VersionID>
                <ns3:ReceivedDateTime>
                  <ns3:DateTimeString formatCode="304">20200219120306Z</ns3:DateTimeString>
                </ns3:ReceivedDateTime>
                <ns3:ICS>22</ns3:ICS>
              </ns3:Declaration>
              <ns5:Declaration xmlns:ns5="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns="http://dmirs.core.ecf/DeclarationInformationRetrieval" xmlns:ns2="urn:wco:datamodel:WCO:Response_DS:DMS:2" xmlns:ns4="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns3="http://gov.uk/customs/declarationInformationRetrieval/status/v2">
                <ns5:FunctionCode>9</ns5:FunctionCode>
                <ns5:TypeCode>IMZ</ns5:TypeCode>
                <ns5:GoodsItemQuantity>1</ns5:GoodsItemQuantity>
                <ns5:TotalPackageQuantity>1.0</ns5:TotalPackageQuantity>
                <ns5:Submitter>
                  <ns5:ID>GB025115166435</ns5:ID>
                </ns5:Submitter>
                <ns5:GoodsShipment>
                  <ns5:PreviousDocument>
                    <ns5:ID>8GB830617936000-0110182</ns5:ID>
                    <ns5:TypeCode>DCR</ns5:TypeCode>
                  </ns5:PreviousDocument>
                  <ns5:UCR>
                    <ns5:TraderAssignedReferenceID>9GB010969918000-0110182</ns5:TraderAssignedReferenceID>
                  </ns5:UCR>
                </ns5:GoodsShipment>
              </ns5:Declaration>
            </v2:retrieveDeclarationStatusDetails>
          </v2:retrieveDeclarationStatusDetailsList>
        </v2:retrieveDeclarationStatusResponse>
      </v2:responseDetail>
    </v2:queryDeclarationStatusResponse>
  }.filter(_ => true)

  val expectedDeclarationStatusPayload: Elem =
    <n1:queryDeclarationInformationRequest
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd_1="http://trade.core.ecf/messages/2017/03/31/"
    xmlns:n1="http://gov.uk/customs/retrieveDeclarationInformation/v1" xmlns:tns_1="http://cmm.core.ecf/BaseTypes/cmmServiceTypes/trade/2017/02/22/"
    xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationInformation/v1 request_schema.xsd">
      <n1:requestCommon>
        <n1:clientID>327d9145-4965-4d28-a2c5-39dedee50334</n1:clientID>
        <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
        <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
        <n1:badgeIdentifier>BADGEID123</n1:badgeIdentifier>
        <n1:dateTimeStamp>2018-09-11T10:28:54.128Z</n1:dateTimeStamp>
        <v1:authenticatedPartyID>ZZ123456789000</v1:authenticatedPartyID>
        <v1:originatingPartyID>BADGEID123</v1:originatingPartyID>
      </n1:requestCommon>
      <n1:requestDetail>
        <n1:declarationManagementInformationRequest>
          <tns_1:id>1b0a48a8-1259-42c9-9d6a-e797b919eb16</tns_1:id>
          <tns_1:timeStamp>2018-09-11T10:28:54.128Z</tns_1:timeStamp>
          <xsd_1:reference>theMrn</xsd_1:reference>
        </n1:declarationManagementInformationRequest>
      </n1:requestDetail>
    </n1:queryDeclarationInformationRequest>

  def generateDeclarationStatusResponse(noOfDeclarationStatusResponses: Int = 1, acceptanceOrCreationDate: DateTime): NodeSeq = {
    val items = 0 until noOfDeclarationStatusResponses
    val content = items.map(index => generateDeclarationStatusDetailsElement(generateHMRCDeclaration(acceptanceOrCreationDate.plusMonths(index)), generateStandardResponseWCODeclaration()))

    generateRootElements(content)
  }

  private def generateRootElements(content: Seq[NodeSeq]): Elem =
    <n1:queryDeclarationStatusResponse
      xmlns:od="urn:wco:datamodel:WCO:DEC-DMS:2"
      xmlns:otnds="urn:wco:datamodel:WCO:Response_DS:DMS:2"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:ds="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
      xmlns:n1="http://gov.uk/customs/declarationInformationRetrieval/status/v2"
      xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v2 queryDeclarationStatusResponse.xsd">
      <n1:responseCommon>
        <n1:processingDate>2001-12-17T09:30:47Z</n1:processingDate>
      </n1:responseCommon>
      <n1:responseDetail>
        <n1:retrieveDeclarationStatusResponse>
          <n1:retrieveDeclarationStatusDetailsList>
            {content}
          </n1:retrieveDeclarationStatusDetailsList>
        </n1:retrieveDeclarationStatusResponse>
      </n1:responseDetail>
    </n1:queryDeclarationStatusResponse>

  private def generateDeclarationStatusDetailsElement(hmrcDeclaration: Node, wcoDeclaration: Node): NodeSeq =
    <n1:retrieveDeclarationStatusDetails>
      {hmrcDeclaration}
      {wcoDeclaration}
    </n1:retrieveDeclarationStatusDetails>


  private def generateHMRCDeclaration(acceptanceOrCreationDate: DateTime, withOptionalElements: Boolean = false): Elem =
    <n1:Declaration>
      <n1:AcceptanceDateTime>
        <otnds:DateTimeString formatCode="304">{acceptanceOrCreationDate.toString(dateTimeFormat)}</otnds:DateTimeString>
      </n1:AcceptanceDateTime>
      {if (withOptionalElements){
        <n1:CancellationDateTime>
          <otnds:DateTimeString formatCode="304">{acceptanceOrCreationDate.toString(dateTimeFormat)}</otnds:DateTimeString>
        </n1:CancellationDateTime>
        <n1:FunctionalReferenceID>token</n1:FunctionalReferenceID>
      }}
      <n1:ID>18GB9JLC3CU1LFGVR2</n1:ID>
      {if (withOptionalElements){
        <n1:RejectionDateTime>
          <otnds:DateTimeString formatCode="304">{acceptanceOrCreationDate.toString(dateTimeFormat)}</otnds:DateTimeString>
        </n1:RejectionDateTime>
      }}
      <n1:VersionID>1</n1:VersionID>
      {if (withOptionalElements){
        <n1:DutyTaxFee>
          <n1:Payment>
            <n1:ReferenceID >token</n1:ReferenceID>
            <n1:TaxAssessedAmount currencyID="GBP">0.0</n1:TaxAssessedAmount>
            <n1:DueDateTime>
              <otnds:DateTimeString formatCode="304">{acceptanceOrCreationDate.toString(dateTimeFormat)}</otnds:DateTimeString>
            </n1:DueDateTime>
            <n1:PaymentAmount currencyID="GBP">0.0</n1:PaymentAmount>
            <n1:ObligationGuarantee>
              <n1:ReferenceID>token</n1:ReferenceID>
            </n1:ObligationGuarantee>
          </n1:Payment>
        </n1:DutyTaxFee>
        <n1:GoodsShipment>
          <n1:GovernmentAgencyGoodsItem>
            <n1:SequenceNumeric>0.0</n1:SequenceNumeric>
            <n1:Commodity>
              <n1:DutyTaxFee/>
            </n1:Commodity>
          </n1:GovernmentAgencyGoodsItem>
        </n1:GoodsShipment>
      }}
      <n1:ReceivedDateTime>
        <n1:DateTimeString formatCode="304">{acceptanceOrCreationDate.plusMinutes(-1).toString(dateTimeFormat)}</n1:DateTimeString>
      </n1:ReceivedDateTime>
      <n1:GoodsReleasedDateTime>
        <n1:DateTimeString formatCode="304">{acceptanceOrCreationDate.plusMinutes(1).toString(dateTimeFormat)}</n1:DateTimeString>
      </n1:GoodsReleasedDateTime>
      <n1:ROE>6</n1:ROE>
      <n1:ICS>15</n1:ICS>
      <n1:IRC>000</n1:IRC>
    </n1:Declaration>

  private def generateStandardResponseWCODeclaration(): Elem =
    <od:Declaration>
      <od:FunctionCode>9</od:FunctionCode>
      <od:TypeCode>IMZ</od:TypeCode>
      <od:GoodsItemQuantity>100</od:GoodsItemQuantity>
      <od:TotalPackageQuantity>10</od:TotalPackageQuantity>
      <od:Submitter>
        <od:ID>GB123456789012000</od:ID>
      </od:Submitter>
      <od:GoodsShipment>
        <od:PreviousDocument>
          <od:ID>18GBAKZ81EQJ2FGVR</od:ID>
          <od:TypeCode>DCR</od:TypeCode>
        </od:PreviousDocument>
        <od:PreviousDocument>
          <od:ID>18GBAKZ81EQJ2FGVA</od:ID>
          <od:TypeCode>MCR</od:TypeCode>
        </od:PreviousDocument>
        <od:UCR>
          <od:TraderAssignedReferenceID>20GBAKZ81EQJ2WXYZ</od:TraderAssignedReferenceID>
        </od:UCR>
      </od:GoodsShipment>
    </od:Declaration>

  def invalidStatusResponse(declarationNode: NodeSeq): NodeSeq =
    <n1:queryDeclarationInformationResponse
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:xsd_1="http://trade.core.ecf/messages/2017/03/31/"
      xmlns:n1="http://gov.uk/customs/retrieveDeclarationInformation/v1"
      xmlns:tns="http://cmm.core.ecf/BaseTypes/cmmPartyTypes/trade/2017/02/22/"
      xmlns:n2="http://cmm.core.ecf/BaseTypes/cmmServiceTypes/trade/2017/02/22/"
      xmlns:n3="http://cmm.core.ecf/BaseTypes/cmmDeclarationTypes/trade/2017/02/22/"
      xmlns:tns_3="http://cmm.core.ecf/BaseTypes/cmmEnhancementTypes/trade/2017/02/22/"
      xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationInformation/v1 queryDeclarationInformationResponse.xsd">
      <n1:responseCommon>
        <n1:processingDate>2001-12-17T09:30:47Z</n1:processingDate>
      </n1:responseCommon>
      <n1:responseDetail>
        <n1:declarationManagementInformationResponse>
          <n2:extensions>
            <tns_3:value>String</tns_3:value>
            <tns_3:type>token</tns_3:type>
          </n2:extensions>
          <n2:sequenceNumber>0</n2:sequenceNumber>
          <n2:request>
            <n2:status>token</n2:status>
            <n2:id>String</n2:id>
            <n2:sequenceNumber>0</n2:sequenceNumber>
            <n2:timeStamp>2001-12-17T09:30:47Z</n2:timeStamp>
            <n2:externalId>String</n2:externalId>
          </n2:request>
          <n2:id>String</n2:id>
          <n2:timeStamp>2001-12-17T09:30:47Z</n2:timeStamp>
          <n2:isFinal>true</n2:isFinal>
          <n2:externalId>String</n2:externalId>
            {declarationNode}
        </n1:declarationManagementInformationResponse>
      </n1:responseDetail>
    </n1:queryDeclarationInformationResponse>

  def generateDeclarationStatusResponseContainingAllOptionalElements(acceptanceOrCreationDate: DateTime): NodeSeq = {
    val content = generateDeclarationStatusDetailsElement(generateHMRCDeclaration(acceptanceOrCreationDate, withOptionalElements = true), getWcoDeclarationWithAllElementsPopulated())

    generateRootElements(content)
  }

  private def getWcoDeclarationWithAllElementsPopulated(): Node = {
    XML.loadFile("test/resources/xml/sample_wco_dec_containing_all_possible_elements.xml").head
  }
}
