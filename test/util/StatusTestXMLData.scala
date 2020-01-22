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

package util

import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.{DateTime, DateTimeFieldType, DateTimeZone}

import scala.xml.{Elem, NodeSeq, XML}

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

  val ValidDeclarationStatusQueryResponseXML: Elem = {
    <p:DeclarationStatusResponse
      xmlns:p="http://gov.uk/customs/declarationInformationRetrieval/status/v2"
      xmlns:p1="urn:wco:datamodel:WCO:Response_DS:DMS:2"
      xmlns:p2="urn:wco:datamodel:WCO:DEC-DMS:2"
      xmlns:p3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
      xmlns:p4="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v2 ../schemas/wco/declaration/DeclarationInformationRetrievalStatusResponse.xsd ">

      <p:DeclarationStatusDetails>
        <p:Declaration>
          <p:ReceivedDateTime>
            <p:DateTimeString formatCode="102">20190702110757Z</p:DateTimeString>
          </p:ReceivedDateTime>
          <p:GoodsReleasedDateTime>
            <p:DateTimeString formatCode="102">20190702110757Z</p:DateTimeString>
          </p:GoodsReleasedDateTime>
          <p:ROE>6</p:ROE>
          <p:ICS>15</p:ICS>
          <p:IRC>000</p:IRC>
          <p:AcceptanceDateTime>
            <p1:DateTimeString formatCode="102">20190702110757Z</p1:DateTimeString>
          </p:AcceptanceDateTime>
          <p:ID>18GB9JLC3CU1LFGVR2</p:ID>
          <p:VersionID>1</p:VersionID>
        </p:Declaration>
        <p2:Declaration>
          <p2:FunctionCode>9</p2:FunctionCode>
          <p2:TypeCode>IMZ</p2:TypeCode>
          <p2:GoodsItemQuantity unitCode="NPR">100</p2:GoodsItemQuantity>
          <p2:TotalPackageQuantity>10</p2:TotalPackageQuantity>
          <p2:Submitter>
            <p2:ID>GB123456789012000</p2:ID>
          </p2:Submitter>
          <p2:GoodsShipment>
            <p2:PreviousDocument>
              <p2:ID>18GBAKZ81EQJ2FGVR</p2:ID>
              <p2:TypeCode>DCR</p2:TypeCode>
            </p2:PreviousDocument>
            <p2:PreviousDocument>
              <p2:ID>18GBAKZ81EQJ2FGVA</p2:ID>
              <p2:TypeCode>MCR</p2:TypeCode>
            </p2:PreviousDocument>
            <p2:UCR>
              <p2:TraderAssignedReferenceID>20GBAKZ81EQJ2WXYZ</p2:TraderAssignedReferenceID>
            </p2:UCR>
          </p2:GoodsShipment>
        </p2:Declaration>
      </p:DeclarationStatusDetails>
    </p:DeclarationStatusResponse>
  }

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
      </n1:requestCommon>
      <n1:requestDetail>
        <n1:declarationManagementInformationRequest>
          <tns_1:id>1b0a48a8-1259-42c9-9d6a-e797b919eb16</tns_1:id>
          <tns_1:timeStamp>2018-09-11T10:28:54.128Z</tns_1:timeStamp>
          <xsd_1:reference>theMrn</xsd_1:reference>
        </n1:declarationManagementInformationRequest>
      </n1:requestDetail>
    </n1:queryDeclarationInformationRequest>

  val generateValidStatusResponseWithMultiplePartiesOnly: NodeSeq = <n1:queryDeclarationInformationResponse xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd_1="http://trade.core.ecf/messages/2017/03/31/" xmlns:n1="http://gov.uk/customs/retrieveDeclarationInformation/v1" xmlns:tns="http://cmm.core.ecf/BaseTypes/cmmPartyTypes/trade/2017/02/22/" xmlns:n2="http://cmm.core.ecf/BaseTypes/cmmServiceTypes/trade/2017/02/22/" xmlns:n3="http://cmm.core.ecf/BaseTypes/cmmDeclarationTypes/trade/2017/02/22/" xmlns:tns_3="http://cmm.core.ecf/BaseTypes/cmmEnhancementTypes/trade/2017/02/22/" xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationInformation/v1 queryDeclarationInformationResponse.xsd">
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
        <xsd_1:declaration>
          <tns_3:isCurrent>true</tns_3:isCurrent>
          <tns_3:isDisplayable>true</tns_3:isDisplayable>
          <n3:extensions>
            <tns_3:value>String</tns_3:value>
            <tns_3:type>token</tns_3:type>
          </n3:extensions>
          <n3:totalGrossMass>0</n3:totalGrossMass>
          <n3:modeOfEntry>token</n3:modeOfEntry>
          <n3:communicationAddress>hmrcgwid:144b80b0-b46e-4c56-be1a-83b36649ac46:ad3a8c50-fc1c-4b81-a56cbb153aced791:BADGEID123</n3:communicationAddress>
          <n3:receiveDate>2001-12-17T09:30:47Z</n3:receiveDate>
          <n3:reference>String</n3:reference>
          <n3:submitterReference>String</n3:submitterReference>
          <n3:goodsCommunityStatus>token</n3:goodsCommunityStatus>
          <n3:loadingListCount>0</n3:loadingListCount>
          <n3:invoiceAmount>0</n3:invoiceAmount>
          <n3:procedureCategory>token</n3:procedureCategory>
          <n3:batchId>String</n3:batchId>
          <n3:specificCircumstance>token</n3:specificCircumstance>
          <xsd_1:additionalDocuments>
            <n3:amount>0</n3:amount>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:quantity>0</n3:quantity>
            <n3:identifier>String</n3:identifier>
            <n3:type>token</n3:type>
            <n3:effectiveDate>2001-12-17T09:30:47Z</n3:effectiveDate>
            <n3:issuerName>String</n3:issuerName>
            <n3:exemption>token</n3:exemption>
            <n3:name>String</n3:name>
          </xsd_1:additionalDocuments>
          <xsd_1:additionalInformation>
            <n3:limitDate>2001-12-17T09:30:47Z</n3:limitDate>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:text>String</n3:text>
            <n3:type>token</n3:type>
            <n3:code>String</n3:code>
            <n3:pointer>String</n3:pointer>
          </xsd_1:additionalInformation>
          <xsd_1:amendments>
            <n3:declarationVersion>0</n3:declarationVersion>
            <n3:effectiveDate>2001-12-17T09:30:47Z</n3:effectiveDate>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:reasonText>String</n3:reasonText>
            <n3:reason>token</n3:reason>
            <n3:pointer>String</n3:pointer>
          </xsd_1:amendments>
          <xsd_1:countryRegions>
            <n3:type>token</n3:type>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:countrySubDivisionId>token</n3:countrySubDivisionId>
            <n3:country>token</n3:country>
            <n3:countryGroup>token</n3:countryGroup>
            <n3:subRole>token</n3:subRole>
          </xsd_1:countryRegions>
          <xsd_1:currencies>
            <tns:exchangeRate>0</tns:exchangeRate>
            <tns:sequenceNumber>0</tns:sequenceNumber>
            <tns:code>token</tns:code>
          </xsd_1:currencies>
          <xsd_1:customsOffices>
            <n3:customsOfficeName>String</n3:customsOfficeName>
            <n3:type>token</n3:type>
            <xsd_1:customsOfficeID>
              <tns:type>token</tns:type>
              <tns:number>String</tns:number>
            </xsd_1:customsOfficeID>
          </xsd_1:customsOffices>
          <xsd_1:guarantees>
            <n3:partyID>String</n3:partyID>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:grn>String</n3:grn>
            <n3:otherReference>String</n3:otherReference>
            <n3:type>token</n3:type>
            <n3:dutyAmount>0</n3:dutyAmount>
            <n3:accessCode>String</n3:accessCode>
            <xsd_1:customsOfficeID>
              <tns:type>token</tns:type>
              <tns:number>String</tns:number>
            </xsd_1:customsOfficeID>
          </xsd_1:guarantees>
          <xsd_1:incidents>
            <n3:text>String</n3:text>
            <n3:sequenceNumber>0</n3:sequenceNumber>
          </xsd_1:incidents>
          <xsd_1:parties>
            <n3:status>token</n3:status>
            <n3:type>TB</n3:type>
            <n3:partyName>String</n3:partyName>
            <n3:subRole>token</n3:subRole>
            <n3:authorizationType>token</n3:authorizationType>
            <xsd_1:partyIdentification>
              <tns:type>token</tns:type>
              <tns:number>1</tns:number>
            </xsd_1:partyIdentification>
            <xsd_1:contactPerson>
              <n3:name>String</n3:name>
              <xsd_1:contactMechanisms>
                <tns_3:invalidDate>2001-12-17T09:30:47Z</tns_3:invalidDate>
                <tns_3:effectiveDate>2001-12-17T09:30:47Z</tns_3:effectiveDate>
                <tns:identification>String</tns:identification>
                <tns:purpose>token</tns:purpose>
                <tns:type>token</tns:type>
              </xsd_1:contactMechanisms>
              <xsd_1:physicalAddress>
                <tns:countrySubDivisionId>String</tns:countrySubDivisionId>
                <tns:type>token</tns:type>
                <tns:streetAndNumber>String</tns:streetAndNumber>
                <tns:countrySubDivisionName>String</tns:countrySubDivisionName>
                <tns:cityName>String</tns:cityName>
                <tns:zipCode>String</tns:zipCode>
                <tns:houseNumberExtension>String</tns:houseNumberExtension>
                <tns:houseNumber>0</tns:houseNumber>
                <tns:streetName>String</tns:streetName>
                <tns:country>
                  <tns:code>token</tns:code>
                </tns:country>
              </xsd_1:physicalAddress>
            </xsd_1:contactPerson>
            <xsd_1:contactMechanisms>
              <tns_3:invalidDate>2001-12-17T09:30:47Z</tns_3:invalidDate>
              <tns_3:effectiveDate>2001-12-17T09:30:47Z</tns_3:effectiveDate>
              <tns:identification>String</tns:identification>
              <tns:purpose>token</tns:purpose>
              <tns:type>token</tns:type>
            </xsd_1:contactMechanisms>
            <xsd_1:physicalAddress>
              <tns:countrySubDivisionId>String</tns:countrySubDivisionId>
              <tns:type>token</tns:type>
              <tns:streetAndNumber>String</tns:streetAndNumber>
              <tns:countrySubDivisionName>String</tns:countrySubDivisionName>
              <tns:cityName>String</tns:cityName>
              <tns:zipCode>String</tns:zipCode>
              <tns:houseNumberExtension>String</tns:houseNumberExtension>
              <tns:houseNumber>0</tns:houseNumber>
              <tns:streetName>String</tns:streetName>
              <tns:country>
                <tns:code>token</tns:code>
              </tns:country>
            </xsd_1:physicalAddress>
          </xsd_1:parties>
          <xsd_1:parties>
            <xsd_1:partyIdentification>
              <tns:number>2</tns:number>
            </xsd_1:partyIdentification>
          </xsd_1:parties>
          <xsd_1:parties></xsd_1:parties>
          <xsd_1:signature>
            <n3:sign>UjBsR09EbGhjZ0dTQUxNQUFBUUNBRU1tQ1p0dU1GUXhEUzhi</n3:sign>
            <n3:name>String</n3:name>
            <n3:place>String</n3:place>
            <n3:timeStamp>2001-12-17T09:30:47Z</n3:timeStamp>
            <n3:authentication>String</n3:authentication>
          </xsd_1:signature>
        </xsd_1:declaration>
      </n1:declarationManagementInformationResponse>
    </n1:responseDetail>
  </n1:queryDeclarationInformationResponse>

  def generateDeclarationStatusResponse(noOfDeclarationStatusResponses: Int = 1, acceptanceOrCreationDate: DateTime): NodeSeq = {
    val items = 0 until noOfDeclarationStatusResponses
    val content = items.map(index => generateDeclarationStatusDetailsElement(acceptanceOrCreationDate.plusMonths(index), generateWCODeclaration()))

    generateRootElements(content)
  }

  def generateRootElements(content: Seq[NodeSeq]): Elem = {
    <n1:queryDeclarationstatusResponse
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
    </n1:queryDeclarationstatusResponse>
  }

  def generateDeclarationStatusDetailsElement(acceptanceOrCreationDate: DateTime, wcoDeclaration: Elem): NodeSeq = {
    <n1:retrieveDeclarationStatusDetails>
      {generateHMRCDeclaration(acceptanceOrCreationDate)}
      {wcoDeclaration}
    </n1:retrieveDeclarationStatusDetails>
  }

  def generateHMRCDeclaration(acceptanceOrCreationDate: DateTime): Elem = {
    <n1:Declaration>
      <n1:AcceptanceDateTime>
        <otnds:DateTimeString formatCode="304">{acceptanceOrCreationDate.toString(dateTimeFormat)}</otnds:DateTimeString>
      </n1:AcceptanceDateTime>
      <n1:ID>18GB9JLC3CU1LFGVR2</n1:ID>
      <n1:VersionID>1</n1:VersionID>
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
  }

  def generateWCODeclaration(): Elem = {
    <od:Declaration>
      <od:FunctionCode>9</od:FunctionCode>
      <od:TypeCode>IMZ</od:TypeCode>
      <od:GoodsItemQuantity unitCode="NPR">100</od:GoodsItemQuantity>
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
  }

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

  /*def generateImportDeclarationStatusResponse(): NodeSeq =
    generateDeclarationStatusResponseFromFile("example_submission_declaration_imports.xml")

  def generateExportDeclarationStatusResponse(): NodeSeq =
    generateDeclarationStatusResponseFromFile("example_submission_declaration_imports.xml")

  private def generateDeclarationStatusResponseFromFile(sampleDeclarationFileName: String): NodeSeq = {
    val xml = XML.loadFile(s"test/resources/sample_xml/$sampleDeclarationFileName")

    val node = (xml \"Declaration")

    val prefixedWCOContent = wcoPrefixReWriter.transform(node.head)
    val content = generateDeclarationStatusDetailsElement(defaultDateTime, prefixedWCOContent.head.asInstanceOf[Elem])
    generateRootElements(content)
  }*/
}
