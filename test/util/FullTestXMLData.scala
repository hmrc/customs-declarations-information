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

import java.time.{LocalDateTime, Month, ZoneId, ZonedDateTime}
import scala.xml.Elem

object FullTestXMLData {
  val defaultDateTime: ZonedDateTime = LocalDateTime.of(2020, Month.JUNE, 15, 12, 30, 0, 0).atZone(ZoneId.of("UTC"))
//  val dateTimeFormat = new DateTimeFormatterBuilder()
//    .appendYear(4, 4)
//    .appendFixedDecimal(DateTimeFieldType.monthOfYear(), 2)
//    .appendFixedDecimal(DateTimeFieldType.dayOfMonth(), 2)
//    .appendFixedDecimal(DateTimeFieldType.hourOfDay, 2)
//    .appendFixedDecimal(DateTimeFieldType.minuteOfHour, 2)
//    .appendFixedDecimal(DateTimeFieldType.secondOfMinute, 2)
//    .appendTimeZoneOffset("Z", false, 2, 2)
//    .toFormatter

  val validNonCspFullRequestPayload =
    """<n1:retrieveFullDeclarationDataRequest xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd"
      |xmlns:n1="http://gov.uk/customs/FullDeclarationDataRetrievalService" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      |          <n1:requestCommon>
      |            <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
      |            <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
      |            <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
      |            <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
      |            <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
      |          </n1:requestCommon>
      |          <n1:requestDetail>
      |                <n1:MRN>theMrn</n1:MRN>
      |                <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>
      |          </n1:requestDetail>
      |        </n1:retrieveFullDeclarationDataRequest>
      |""".stripMargin

  val validNonCspFullRequestPayloadWithoutDeclarationVersion =
    """<n1:retrieveFullDeclarationDataRequest xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd"
      |xmlns:n1="http://gov.uk/customs/FullDeclarationDataRetrievalService" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      |          <n1:requestCommon>
      |            <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
      |            <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
      |            <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
      |            <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
      |            <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
      |          </n1:requestCommon>
      |          <n1:requestDetail>
      |                <n1:MRN>theMrn</n1:MRN>
      |                <n1:DeclarationSubmissionChannel>AuthenticatedPartyOnly</n1:DeclarationSubmissionChannel>
      |          </n1:requestDetail>
      |        </n1:retrieveFullDeclarationDataRequest>
      |""".stripMargin

  val validNonCspFullRequestPayloadWithDeclarationSubmissionChannel =
    """<n1:retrieveFullDeclarationDataRequest xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd"
      |xmlns:n1="http://gov.uk/customs/FullDeclarationDataRetrievalService" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      |          <n1:requestCommon>
      |            <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>
      |            <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
      |            <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
      |            <n1:dateTimeStamp>2017-06-08T13:55:00.000Z</n1:dateTimeStamp>
      |            <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>
      |          </n1:requestCommon>
      |          <n1:requestDetail>
      |                <n1:MRN>theMrn</n1:MRN>
      |                <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>
      |                <n1:DeclarationSubmissionChannel>AuthenticatedPartyOnly</n1:DeclarationSubmissionChannel>
      |          </n1:requestDetail>
      |        </n1:retrieveFullDeclarationDataRequest>
      |""".stripMargin

  val validCspFullRequestPayload =
    """<n1:retrieveFullDeclarationDataRequest xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd"
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
      |                <n1:MRN>theMrn</n1:MRN>
      |                <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>
      |          </n1:requestDetail>
      |        </n1:retrieveFullDeclarationDataRequest>
      |""".stripMargin

  val validCspFullRequestPayloadWithDeclarationSubmissionChannel =
    """<n1:retrieveFullDeclarationDataRequest xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd"
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
      |                <n1:MRN>theMrn</n1:MRN>
      |                <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>
      |                <n1:DeclarationSubmissionChannel>AuthenticatedPartyOnly</n1:DeclarationSubmissionChannel>
      |          </n1:requestDetail>
      |        </n1:retrieveFullDeclarationDataRequest>
      |""".stripMargin

  val validCspFullRequestWithoutBadgePayload =
    """<n1:retrieveFullDeclarationDataRequest xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd"
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
      |                <n1:MRN>theMrn</n1:MRN>
      |                <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>
      |          </n1:requestDetail>
      |        </n1:retrieveFullDeclarationDataRequest>
      |""".stripMargin

  val expectedFullPayloadRequest = {
    <n1:retrieveFullDeclarationDataRequest
    xmlns:xsi="http://gov.uk/customs/retrieveFullDeclarationDataRequest"
    xmlns:n1="http://gov.uk/customs/FullDeclarationDataRetrievalService"
    xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd">
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
            <n1:MRN>theMrn</n1:MRN>
            <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>
      </n1:requestDetail>
    </n1:retrieveFullDeclarationDataRequest>
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
    xmlns:urn1="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:ds="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:n1="http://gov.uk/customs/FullDeclarationDataRetrievalService">
      <n1:responseCommon>
        <n1:processingDate>2024-01-11T11:57:18Z</n1:processingDate>
      </n1:responseCommon>
      <n1:responseDetail>
        <n1:FullDeclarationDataRetrievalResponse>
          <n1:FullDeclarationDataDetails>
            <n1:HighLevelSummaryDetails>
              <n1:MRN>24GB0EZYSZ6FUI6AR5</n1:MRN>
              <n1:LRN>SK383RefTC181788</n1:LRN>
              <n1:DUCRandPartID>12345</n1:DUCRandPartID>
              <n1:VersionID>1</n1:VersionID>
              <n1:GoodsLocationCode>GBBYCSEBELLCA</n1:GoodsLocationCode>
              <n1:CreatedDateTime>
                <n1:DateTimeString formatCode="304">20240111115536Z</n1:DateTimeString>
              </n1:CreatedDateTime>
              <n1:AcceptanceDateTime>
                <n1:DateTimeString formatCode="304">20240111115536Z</n1:DateTimeString>
              </n1:AcceptanceDateTime>
            </n1:HighLevelSummaryDetails>
            <n1:AccountDetails>
              <n1:DAN>
                <n1:Identifier>7121503</n1:Identifier>
                <n1:Type>1DAN</n1:Type>
              </n1:DAN>
              <n1:MOP>2</n1:MOP>
              <n1:DutyTaxFee>
                <n1:TaxType>B00</n1:TaxType>
                <n1:Amount>423.44</n1:Amount>
                <n1:CoverageAmountType>8</n1:CoverageAmountType>
              </n1:DutyTaxFee>
              <n1:DutyTaxFee>
                <n1:TaxType>A00</n1:TaxType>
                <n1:Amount>617.24</n1:Amount>
                <n1:CoverageAmountType>7</n1:CoverageAmountType>
              </n1:DutyTaxFee>
            </n1:AccountDetails>
            <n1:GeneratedConsignmentDetails>
              <n1:ROE>6</n1:ROE>
              <n1:StatusOfEntry-ICS>2</n1:StatusOfEntry-ICS>
              <n1:GoodsArrivalDateTime>
                <n1:DateTimeString formatCode="304">20240111115538Z</n1:DateTimeString>
              </n1:GoodsArrivalDateTime>
              <n1:SubmitterID>GB111222333444</n1:SubmitterID>
              <n1:StatisticalValue currencyID="GBP">100.0</n1:StatisticalValue>
              <n1:TotalInvoiceAmount currencyID="GBP">1500.0</n1:TotalInvoiceAmount>
            </n1:GeneratedConsignmentDetails>
            <n1:GeneratedItemLevelConsignmentDetails>
              <n1:ItemNumber>1</n1:ItemNumber>
              <n1:QuotaNumber>050067</n1:QuotaNumber>
              <n1:VATValue currencyID="GBP">423.44</n1:VATValue>
              <n1:VATRate unitCode="P1">20.0</n1:VATRate>
              <n1:CustomsValue currencyID="GBP">1500.0</n1:CustomsValue>
              <n1:DutyTaxFee>
                <n1:TaxType>A00</n1:TaxType>
                <n1:Amount currencyID="GBP">617.24</n1:Amount>
              </n1:DutyTaxFee>
              <n1:DutyTaxFee>
                <n1:TaxBase unitCode="GBP">2117.24</n1:TaxBase>
                <n1:TaxType>B00</n1:TaxType>
                <n1:Amount currencyID="GBP">423.44</n1:Amount>
                <n1:TaxRate unitCode="P1">20.0</n1:TaxRate>
              </n1:DutyTaxFee>
            </n1:GeneratedItemLevelConsignmentDetails>
            <n1:FullDeclarationObject>
              <n1:Declaration>
                <n1:AcceptanceDateTime>
                  <n1:DateTimeString formatCode="304">20240111115536Z</n1:DateTimeString>
                </n1:AcceptanceDateTime>
                <n1:FunctionCode>9</n1:FunctionCode>
                <n1:FunctionalReferenceID>SK383RefTC181788</n1:FunctionalReferenceID>
                <n1:TypeCode>IMA</n1:TypeCode>
                <n1:ProcedureCategory>H1</n1:ProcedureCategory>
                <n1:GoodsItemQuantity>1</n1:GoodsItemQuantity>
                <n1:InvoiceAmount currencyID="GBP">1500.0</n1:InvoiceAmount>
                <n1:TotalPackageQuantity>1</n1:TotalPackageQuantity>
                <n1:AdditionalDocument>
                  <n1:SequenceNumeric>1</n1:SequenceNumeric>
                  <n1:CategoryCode>1</n1:CategoryCode>
                  <n1:ID>7121503</n1:ID>
                  <n1:TypeCode>DAN</n1:TypeCode>
                </n1:AdditionalDocument>
                <n1:AuthorisationHolder>
                  <n1:ID>GB519594455000</n1:ID>
                  <n1:CategoryCode>IPO</n1:CategoryCode>
                </n1:AuthorisationHolder>
                <n1:AuthorisationHolder>
                  <n1:ID>GB519594455000</n1:ID>
                  <n1:CategoryCode>DPO</n1:CategoryCode>
                </n1:AuthorisationHolder>
                <n1:AuthorisationHolder>
                  <n1:ID>GB519594455000</n1:ID>
                  <n1:CategoryCode>CWP</n1:CategoryCode>
                </n1:AuthorisationHolder>
                <n1:AuthorisationHolder>
                  <n1:ID>GB519594455000</n1:ID>
                  <n1:CategoryCode>CGU</n1:CategoryCode>
                </n1:AuthorisationHolder>
                <n1:AuthorisationHolder>
                  <n1:ID>GB519594455000</n1:ID>
                  <n1:CategoryCode>EUS</n1:CategoryCode>
                </n1:AuthorisationHolder>
                <n1:AuthorisationHolder>
                  <n1:ID>GB519594455000</n1:ID>
                  <n1:CategoryCode>AEOC</n1:CategoryCode>
                </n1:AuthorisationHolder>
                <n1:BorderTransportMeans>
                  <n1:RegistrationNationalityCode>NL</n1:RegistrationNationalityCode>
                  <n1:ModeCode>4</n1:ModeCode>
                </n1:BorderTransportMeans>
                <n1:Declarant>
                  <n1:ID>GB519594455000</n1:ID>
                  <n1:Address/>
                </n1:Declarant>
                <n1:Exporter>
                  <n1:Name>Fabulous Automotive Parts</n1:Name>
                  <n1:Address>
                    <n1:CityName>Shandong</n1:CityName>
                    <n1:CountryCode>CN</n1:CountryCode>
                    <n1:Line>Shandong</n1:Line>
                    <n1:PostcodeID>NA</n1:PostcodeID>
                  </n1:Address>
                </n1:Exporter>
                <n1:GoodsShipment>
                  <n1:TransactionNatureCode>1</n1:TransactionNatureCode>
                  <n1:Consignment>
                    <n1:ContainerCode>1</n1:ContainerCode>
                    <n1:GoodsLocation>
                      <n1:Name>CSEBELLCA</n1:Name>
                      <n1:ID>U1234567GB</n1:ID>
                      <n1:TypeCode>B</n1:TypeCode>
                      <n1:Address>
                        <n1:TypeCode>Y</n1:TypeCode>
                        <n1:CountryCode>GB</n1:CountryCode>
                      </n1:Address>
                    </n1:GoodsLocation>
                  </n1:Consignment>
                  <n1:Destination>
                    <n1:CountryCode>GB</n1:CountryCode>
                  </n1:Destination>
                  <n1:ExportCountry>
                    <n1:ID>AD</n1:ID>
                  </n1:ExportCountry>
                  <n1:GovernmentAgencyGoodsItem>
                    <n1:SequenceNumeric>1</n1:SequenceNumeric>
                    <n1:StatisticalValueAmount currencyID="GBP">100.0</n1:StatisticalValueAmount>
                    <n1:TransactionNatureCode>1</n1:TransactionNatureCode>
                    <n1:AdditionalDocument>
                      <n1:SequenceNumeric>1</n1:SequenceNumeric>
                      <n1:CategoryCode>N</n1:CategoryCode>
                      <n1:ID>123457</n1:ID>
                      <n1:TypeCode>935</n1:TypeCode>
                      <n1:LPCOExemptionCode>AC</n1:LPCOExemptionCode>
                    </n1:AdditionalDocument>
                    <n1:AdditionalDocument>
                      <n1:SequenceNumeric>2</n1:SequenceNumeric>
                      <n1:CategoryCode>C</n1:CategoryCode>
                      <n1:ID>GBAEOC 90218/20</n1:ID>
                      <n1:TypeCode>501</n1:TypeCode>
                    </n1:AdditionalDocument>
                    <n1:AdditionalDocument>
                      <n1:SequenceNumeric>3</n1:SequenceNumeric>
                      <n1:CategoryCode>C</n1:CategoryCode>
                      <n1:ID>GBCWP51959445500020201104085600</n1:ID>
                      <n1:TypeCode>517</n1:TypeCode>
                    </n1:AdditionalDocument>
                    <n1:AdditionalDocument>
                      <n1:SequenceNumeric>4</n1:SequenceNumeric>
                      <n1:CategoryCode>C</n1:CategoryCode>
                      <n1:ID>GBDPO7121503</n1:ID>
                      <n1:TypeCode>506</n1:TypeCode>
                    </n1:AdditionalDocument>
                    <n1:AdditionalDocument>
                      <n1:SequenceNumeric>5</n1:SequenceNumeric>
                      <n1:CategoryCode>C</n1:CategoryCode>
                      <n1:ID>GBCGUguaranteenotrequired-CCC</n1:ID>
                      <n1:TypeCode>505</n1:TypeCode>
                      <n1:LPCOExemptionCode>CC</n1:LPCOExemptionCode>
                    </n1:AdditionalDocument>
                    <n1:AdditionalDocument>
                      <n1:SequenceNumeric>6</n1:SequenceNumeric>
                      <n1:CategoryCode>A</n1:CategoryCode>
                      <n1:ID>GBSDE196222540259</n1:ID>
                      <n1:TypeCode>015</n1:TypeCode>
                      <n1:LPCOExemptionCode>AS</n1:LPCOExemptionCode>
                    </n1:AdditionalDocument>
                    <n1:AdditionalDocument>
                      <n1:SequenceNumeric>7</n1:SequenceNumeric>
                      <n1:CategoryCode>C</n1:CategoryCode>
                      <n1:ID>GBIPO51959445500020201104085600</n1:ID>
                      <n1:Name>Test</n1:Name>
                      <n1:TypeCode>601</n1:TypeCode>
                      <n1:LPCOExemptionCode>XW</n1:LPCOExemptionCode>
                    </n1:AdditionalDocument>
                    <n1:AdditionalDocument>
                      <n1:SequenceNumeric>8</n1:SequenceNumeric>
                      <n1:CategoryCode>N</n1:CategoryCode>
                      <n1:ID>GB796458895001</n1:ID>
                      <n1:Name>Test</n1:Name>
                      <n1:TypeCode>853</n1:TypeCode>
                      <n1:LPCOExemptionCode>JE</n1:LPCOExemptionCode>
                    </n1:AdditionalDocument>
                    <n1:AdditionalDocument>
                      <n1:SequenceNumeric>9</n1:SequenceNumeric>
                      <n1:CategoryCode>N</n1:CategoryCode>
                      <n1:ID>GB AEOF 12344/09</n1:ID>
                      <n1:TypeCode>990</n1:TypeCode>
                    </n1:AdditionalDocument>
                    <n1:AdditionalDocument>
                      <n1:SequenceNumeric>10</n1:SequenceNumeric>
                      <n1:CategoryCode>Y</n1:CategoryCode>
                      <n1:ID>GBCGU02111122900020200828141500</n1:ID>
                      <n1:Name>Test</n1:Name>
                      <n1:TypeCode>929</n1:TypeCode>
                    </n1:AdditionalDocument>
                    <n1:AdditionalDocument>
                      <n1:SequenceNumeric>11</n1:SequenceNumeric>
                      <n1:CategoryCode>9</n1:CategoryCode>
                      <n1:ID>GBSDE534209-9120</n1:ID>
                      <n1:TypeCode>120</n1:TypeCode>
                      <n1:LPCOExemptionCode>AE</n1:LPCOExemptionCode>
                    </n1:AdditionalDocument>
                    <n1:AdditionalInformation>
                      <n1:SequenceNumeric>1</n1:SequenceNumeric>
                      <n1:StatementCode>00500</n1:StatementCode>
                      <n1:StatementDescription>test</n1:StatementDescription>
                    </n1:AdditionalInformation>
                    <n1:Commodity>
                      <n1:Description>Alloy Wheels</n1:Description>
                      <n1:Classification>
                        <n1:ID>11</n1:ID>
                        <n1:IdentificationTypeCode>TRC</n1:IdentificationTypeCode>
                      </n1:Classification>
                      <n1:Classification>
                        <n1:ID>VATZ</n1:ID>
                        <n1:IdentificationTypeCode>GN</n1:IdentificationTypeCode>
                      </n1:Classification>
                      <n1:Classification>
                        <n1:ID>2500</n1:ID>
                        <n1:IdentificationTypeCode>TRA</n1:IdentificationTypeCode>
                      </n1:Classification>
                      <n1:Classification>
                        <n1:ID>20096911</n1:ID>
                        <n1:IdentificationTypeCode>TSP</n1:IdentificationTypeCode>
                      </n1:Classification>
                      <n1:DutyTaxFee>
                        <n1:SequenceNumeric>1</n1:SequenceNumeric>
                        <n1:DutyRegimeCode>123</n1:DutyRegimeCode>
                        <n1:TypeCode>A30</n1:TypeCode>
                        <n1:QuotaOrderID>050067</n1:QuotaOrderID>
                        <n1:Payment>
                          <n1:MethodCode>E</n1:MethodCode>
                        </n1:Payment>
                      </n1:DutyTaxFee>
                      <n1:GoodsMeasure>
                        <n1:GrossMassMeasure unitCode="KGM">200.0</n1:GrossMassMeasure>
                        <n1:NetNetWeightMeasure unitCode="KGM">100.0</n1:NetNetWeightMeasure>
                        <n1:TariffQuantity>100.0</n1:TariffQuantity>
                      </n1:GoodsMeasure>
                      <n1:InvoiceLine>
                        <n1:ItemChargeAmount currencyID="GBP">1500.0</n1:ItemChargeAmount>
                      </n1:InvoiceLine>
                      <n1:TransportEquipment>
                        <n1:SequenceNumeric>1</n1:SequenceNumeric>
                        <n1:ID>APHU7215244</n1:ID>
                      </n1:TransportEquipment>
                    </n1:Commodity>
                    <n1:GovernmentProcedure>
                      <n1:CurrentCode>44</n1:CurrentCode>
                      <n1:PreviousCode>00</n1:PreviousCode>
                    </n1:GovernmentProcedure>
                    <n1:GovernmentProcedure>
                      <n1:CurrentCode>000</n1:CurrentCode>
                    </n1:GovernmentProcedure>
                    <n1:Origin>
                      <n1:CountryCode>TR</n1:CountryCode>
                      <n1:TypeCode>1</n1:TypeCode>
                    </n1:Origin>
                    <n1:Packaging>
                      <n1:SequenceNumeric>1</n1:SequenceNumeric>
                      <n1:MarksNumbersID>ADR</n1:MarksNumbersID>
                      <n1:QuantityQuantity>788</n1:QuantityQuantity>
                      <n1:TypeCode>CT</n1:TypeCode>
                    </n1:Packaging>
                    <n1:ValuationAdjustment>
                      <n1:AdditionCode>0000</n1:AdditionCode>
                    </n1:ValuationAdjustment>
                  </n1:GovernmentAgencyGoodsItem>
                  <n1:Importer>
                    <n1:ID>GB519594455000</n1:ID>
                    <n1:Address/>
                  </n1:Importer>
                  <n1:PreviousDocument>
                    <n1:SequenceNumeric>2</n1:SequenceNumeric>
                    <n1:CategoryCode>Y</n1:CategoryCode>
                    <n1:ID>12345</n1:ID>
                    <n1:TypeCode>DCR</n1:TypeCode>
                    <n1:LineNumeric>1</n1:LineNumeric>
                  </n1:PreviousDocument>
                  <n1:PreviousDocument>
                    <n1:SequenceNumeric>1</n1:SequenceNumeric>
                    <n1:CategoryCode>Y</n1:CategoryCode>
                    <n1:ID>201710017INSERTEORI-12345</n1:ID>
                    <n1:TypeCode>DCR</n1:TypeCode>
                    <n1:LineNumeric>1</n1:LineNumeric>
                  </n1:PreviousDocument>
                  <n1:TradeTerms>
                    <n1:ConditionCode>DDP</n1:ConditionCode>
                    <n1:LocationID>GBTIL</n1:LocationID>
                  </n1:TradeTerms>
                  <n1:UCR>
                    <n1:TraderAssignedReferenceID>7INSERTEORI-12345</n1:TraderAssignedReferenceID>
                  </n1:UCR>
                  <n1:Warehouse>
                    <n1:ID>8887776668</n1:ID>
                    <n1:TypeCode>U</n1:TypeCode>
                  </n1:Warehouse>
                </n1:GoodsShipment>
                <n1:ObligationGuarantee>
                  <n1:SequenceNumeric>1</n1:SequenceNumeric>
                  <n1:ID>7121503</n1:ID>
                  <n1:SecurityDetailsCode>0</n1:SecurityDetailsCode>
                </n1:ObligationGuarantee>
                <n1:SupervisingOffice>
                  <n1:ID>GBBEL004</n1:ID>
                </n1:SupervisingOffice>
              </n1:Declaration>
            </n1:FullDeclarationObject>
          </n1:FullDeclarationDataDetails>
        </n1:FullDeclarationDataRetrievalResponse>
      </n1:responseDetail>
    </n1:fullDeclarationDataResponse>


  val validFilteredFullResponseXML: Elem = {
    <p:DeclarationFullResponse xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:p4="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6" xmlns:p3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:p2="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:p1="urn:wco:datamodel:WCO:Response_DS:DMS:2" xmlns:p="http://gov.uk/customs/FullDeclarationDataRetrievalService" xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService">
      <p:FullDeclarationDataDetails>
        <p:HighLevelSummaryDetails>
          <p:MRN>24GB0EZYSZ6FUI6AR5</p:MRN>
          <p:LRN>SK383RefTC181788</p:LRN>
          <p:DUCRandPartID>12345</p:DUCRandPartID>
          <p:VersionID>1</p:VersionID>
          <p:GoodsLocationCode>GBBYCSEBELLCA</p:GoodsLocationCode>
          <p:CreatedDateTime>
            <p:DateTimeString formatCode="304">20240111115536Z</p:DateTimeString>
          </p:CreatedDateTime>
          <p:AcceptanceDateTime>
            <p:DateTimeString formatCode="304">20240111115536Z</p:DateTimeString>
          </p:AcceptanceDateTime>
        </p:HighLevelSummaryDetails>
        <p:AccountDetails>
          <p:DAN>
            <p:Identifier>7121503</p:Identifier>
            <p:Type>1DAN</p:Type>
          </p:DAN>
          <p:MOP>2</p:MOP>
          <p:DutyTaxFee>
            <p:TaxType>B00</p:TaxType>
            <p:Amount>423.44</p:Amount>
            <p:CoverageAmountType>8</p:CoverageAmountType>
          </p:DutyTaxFee>
          <p:DutyTaxFee>
            <p:TaxType>A00</p:TaxType>
            <p:Amount>617.24</p:Amount>
            <p:CoverageAmountType>7</p:CoverageAmountType>
          </p:DutyTaxFee>
        </p:AccountDetails>
        <p:GeneratedConsignmentDetails>
          <p:ROE>6</p:ROE>
          <p:StatusOfEntry-ICS>2</p:StatusOfEntry-ICS>
          <p:GoodsArrivalDateTime>
            <p:DateTimeString formatCode="304">20240111115538Z</p:DateTimeString>
          </p:GoodsArrivalDateTime>
          <p:SubmitterID>GB111222333444</p:SubmitterID>
          <p:StatisticalValue currencyID="GBP">100.0</p:StatisticalValue>
          <p:TotalInvoiceAmount currencyID="GBP">1500.0</p:TotalInvoiceAmount>
        </p:GeneratedConsignmentDetails>
        <p:GeneratedItemLevelConsignmentDetails>
          <p:ItemNumber>1</p:ItemNumber>
          <p:QuotaNumber>050067</p:QuotaNumber>
          <p:VATValue currencyID="GBP">423.44</p:VATValue>
          <p:VATRate unitCode="P1">20.0</p:VATRate>
          <p:CustomsValue currencyID="GBP">1500.0</p:CustomsValue>
          <p:DutyTaxFee>
            <p:TaxType>A00</p:TaxType>
            <p:Amount currencyID="GBP">617.24</p:Amount>
          </p:DutyTaxFee>
          <p:DutyTaxFee>
            <p:TaxBase unitCode="GBP">2117.24</p:TaxBase>
            <p:TaxType>B00</p:TaxType>
            <p:Amount currencyID="GBP">423.44</p:Amount>
            <p:TaxRate unitCode="P1">20.0</p:TaxRate>
          </p:DutyTaxFee>
        </p:GeneratedItemLevelConsignmentDetails>
        <p:FullDeclarationObject>
          <p:Declaration>
            <p:AcceptanceDateTime>
              <p:DateTimeString formatCode="304">20240111115536Z</p:DateTimeString>
            </p:AcceptanceDateTime>
            <p:FunctionCode>9</p:FunctionCode>
            <p:FunctionalReferenceID>SK383RefTC181788</p:FunctionalReferenceID>
            <p:TypeCode>IMA</p:TypeCode>
            <p:ProcedureCategory>H1</p:ProcedureCategory>
            <p:GoodsItemQuantity>1</p:GoodsItemQuantity>
            <p:InvoiceAmount currencyID="GBP">1500.0</p:InvoiceAmount>
            <p:TotalPackageQuantity>1</p:TotalPackageQuantity>
            <p:AdditionalDocument>
              <p:SequenceNumeric>1</p:SequenceNumeric>
              <p:CategoryCode>1</p:CategoryCode>
              <p:ID>7121503</p:ID>
              <p:TypeCode>DAN</p:TypeCode>
            </p:AdditionalDocument>
            <p:AuthorisationHolder>
              <p:ID>GB519594455000</p:ID>
              <p:CategoryCode>IPO</p:CategoryCode>
            </p:AuthorisationHolder>
            <p:AuthorisationHolder>
              <p:ID>GB519594455000</p:ID>
              <p:CategoryCode>DPO</p:CategoryCode>
            </p:AuthorisationHolder>
            <p:AuthorisationHolder>
              <p:ID>GB519594455000</p:ID>
              <p:CategoryCode>CWP</p:CategoryCode>
            </p:AuthorisationHolder>
            <p:AuthorisationHolder>
              <p:ID>GB519594455000</p:ID>
              <p:CategoryCode>CGU</p:CategoryCode>
            </p:AuthorisationHolder>
            <p:AuthorisationHolder>
              <p:ID>GB519594455000</p:ID>
              <p:CategoryCode>EUS</p:CategoryCode>
            </p:AuthorisationHolder>
            <p:AuthorisationHolder>
              <p:ID>GB519594455000</p:ID>
              <p:CategoryCode>AEOC</p:CategoryCode>
            </p:AuthorisationHolder>
            <p:BorderTransportMeans>
              <p:RegistrationNationalityCode>NL</p:RegistrationNationalityCode>
              <p:ModeCode>4</p:ModeCode>
            </p:BorderTransportMeans>
            <p:Declarant>
              <p:ID>GB519594455000</p:ID>
              <p:Address/>
            </p:Declarant>
            <p:Exporter>
              <p:Name>Fabulous Automotive Parts</p:Name>
              <p:Address>
                <p:CityName>Shandong</p:CityName>
                <p:CountryCode>CN</p:CountryCode>
                <p:Line>Shandong</p:Line>
                <p:PostcodeID>NA</p:PostcodeID>
              </p:Address>
            </p:Exporter>
            <p:GoodsShipment>
              <p:TransactionNatureCode>1</p:TransactionNatureCode>
              <p:Consignment>
                <p:ContainerCode>1</p:ContainerCode>
                <p:GoodsLocation>
                  <p:Name>CSEBELLCA</p:Name>
                  <p:ID>U1234567GB</p:ID>
                  <p:TypeCode>B</p:TypeCode>
                  <p:Address>
                    <p:TypeCode>Y</p:TypeCode>
                    <p:CountryCode>GB</p:CountryCode>
                  </p:Address>
                </p:GoodsLocation>
              </p:Consignment>
              <p:Destination>
                <p:CountryCode>GB</p:CountryCode>
              </p:Destination>
              <p:ExportCountry>
                <p:ID>AD</p:ID>
              </p:ExportCountry>
              <p:GovernmentAgencyGoodsItem>
                <p:SequenceNumeric>1</p:SequenceNumeric>
                <p:StatisticalValueAmount currencyID="GBP">100.0</p:StatisticalValueAmount>
                <p:TransactionNatureCode>1</p:TransactionNatureCode>
                <p:AdditionalDocument>
                  <p:SequenceNumeric>1</p:SequenceNumeric>
                  <p:CategoryCode>N</p:CategoryCode>
                  <p:ID>123457</p:ID>
                  <p:TypeCode>935</p:TypeCode>
                  <p:LPCOExemptionCode>AC</p:LPCOExemptionCode>
                </p:AdditionalDocument>
                <p:AdditionalDocument>
                  <p:SequenceNumeric>2</p:SequenceNumeric>
                  <p:CategoryCode>C</p:CategoryCode>
                  <p:ID>GBAEOC 90218/20</p:ID>
                  <p:TypeCode>501</p:TypeCode>
                </p:AdditionalDocument>
                <p:AdditionalDocument>
                  <p:SequenceNumeric>3</p:SequenceNumeric>
                  <p:CategoryCode>C</p:CategoryCode>
                  <p:ID>GBCWP51959445500020201104085600</p:ID>
                  <p:TypeCode>517</p:TypeCode>
                </p:AdditionalDocument>
                <p:AdditionalDocument>
                  <p:SequenceNumeric>4</p:SequenceNumeric>
                  <p:CategoryCode>C</p:CategoryCode>
                  <p:ID>GBDPO7121503</p:ID>
                  <p:TypeCode>506</p:TypeCode>
                </p:AdditionalDocument>
                <p:AdditionalDocument>
                  <p:SequenceNumeric>5</p:SequenceNumeric>
                  <p:CategoryCode>C</p:CategoryCode>
                  <p:ID>GBCGUguaranteenotrequired-CCC</p:ID>
                  <p:TypeCode>505</p:TypeCode>
                  <p:LPCOExemptionCode>CC</p:LPCOExemptionCode>
                </p:AdditionalDocument>
                <p:AdditionalDocument>
                  <p:SequenceNumeric>6</p:SequenceNumeric>
                  <p:CategoryCode>A</p:CategoryCode>
                  <p:ID>GBSDE196222540259</p:ID>
                  <p:TypeCode>015</p:TypeCode>
                  <p:LPCOExemptionCode>AS</p:LPCOExemptionCode>
                </p:AdditionalDocument>
                <p:AdditionalDocument>
                  <p:SequenceNumeric>7</p:SequenceNumeric>
                  <p:CategoryCode>C</p:CategoryCode>
                  <p:ID>GBIPO51959445500020201104085600</p:ID>
                  <p:Name>Test</p:Name>
                  <p:TypeCode>601</p:TypeCode>
                  <p:LPCOExemptionCode>XW</p:LPCOExemptionCode>
                </p:AdditionalDocument>
                <p:AdditionalDocument>
                  <p:SequenceNumeric>8</p:SequenceNumeric>
                  <p:CategoryCode>N</p:CategoryCode>
                  <p:ID>GB796458895001</p:ID>
                  <p:Name>Test</p:Name>
                  <p:TypeCode>853</p:TypeCode>
                  <p:LPCOExemptionCode>JE</p:LPCOExemptionCode>
                </p:AdditionalDocument>
                <p:AdditionalDocument>
                  <p:SequenceNumeric>9</p:SequenceNumeric>
                  <p:CategoryCode>N</p:CategoryCode>
                  <p:ID>GB AEOF 12344/09</p:ID>
                  <p:TypeCode>990</p:TypeCode>
                </p:AdditionalDocument>
                <p:AdditionalDocument>
                  <p:SequenceNumeric>10</p:SequenceNumeric>
                  <p:CategoryCode>Y</p:CategoryCode>
                  <p:ID>GBCGU02111122900020200828141500</p:ID>
                  <p:Name>Test</p:Name>
                  <p:TypeCode>929</p:TypeCode>
                </p:AdditionalDocument>
                <p:AdditionalDocument>
                  <p:SequenceNumeric>11</p:SequenceNumeric>
                  <p:CategoryCode>9</p:CategoryCode>
                  <p:ID>GBSDE534209-9120</p:ID>
                  <p:TypeCode>120</p:TypeCode>
                  <p:LPCOExemptionCode>AE</p:LPCOExemptionCode>
                </p:AdditionalDocument>
                <p:AdditionalInformation>
                  <p:SequenceNumeric>1</p:SequenceNumeric>
                  <p:StatementCode>00500</p:StatementCode>
                  <p:StatementDescription>test</p:StatementDescription>
                </p:AdditionalInformation>
                <p:Commodity>
                  <p:Description>Alloy Wheels</p:Description>
                  <p:Classification>
                    <p:ID>11</p:ID>
                    <p:IdentificationTypeCode>TRC</p:IdentificationTypeCode>
                  </p:Classification>
                  <p:Classification>
                    <p:ID>VATZ</p:ID>
                    <p:IdentificationTypeCode>GN</p:IdentificationTypeCode>
                  </p:Classification>
                  <p:Classification>
                    <p:ID>2500</p:ID>
                    <p:IdentificationTypeCode>TRA</p:IdentificationTypeCode>
                  </p:Classification>
                  <p:Classification>
                    <p:ID>20096911</p:ID>
                    <p:IdentificationTypeCode>TSP</p:IdentificationTypeCode>
                  </p:Classification>
                  <p:DutyTaxFee>
                    <p:SequenceNumeric>1</p:SequenceNumeric>
                    <p:DutyRegimeCode>123</p:DutyRegimeCode>
                    <p:TypeCode>A30</p:TypeCode>
                    <p:QuotaOrderID>050067</p:QuotaOrderID>
                    <p:Payment>
                      <p:MethodCode>E</p:MethodCode>
                    </p:Payment>
                  </p:DutyTaxFee>
                  <p:GoodsMeasure>
                    <p:GrossMassMeasure unitCode="KGM">200.0</p:GrossMassMeasure>
                    <p:NetNetWeightMeasure unitCode="KGM">100.0</p:NetNetWeightMeasure>
                    <p:TariffQuantity>100.0</p:TariffQuantity>
                  </p:GoodsMeasure>
                  <p:InvoiceLine>
                    <p:ItemChargeAmount currencyID="GBP">1500.0</p:ItemChargeAmount>
                  </p:InvoiceLine>
                  <p:TransportEquipment>
                    <p:SequenceNumeric>1</p:SequenceNumeric>
                    <p:ID>APHU7215244</p:ID>
                  </p:TransportEquipment>
                </p:Commodity>
                <p:GovernmentProcedure>
                  <p:CurrentCode>44</p:CurrentCode>
                  <p:PreviousCode>00</p:PreviousCode>
                </p:GovernmentProcedure>
                <p:GovernmentProcedure>
                  <p:CurrentCode>000</p:CurrentCode>
                </p:GovernmentProcedure>
                <p:Origin>
                  <p:CountryCode>TR</p:CountryCode>
                  <p:TypeCode>1</p:TypeCode>
                </p:Origin>
                <p:Packaging>
                  <p:SequenceNumeric>1</p:SequenceNumeric>
                  <p:MarksNumbersID>ADR</p:MarksNumbersID>
                  <p:QuantityQuantity>788</p:QuantityQuantity>
                  <p:TypeCode>CT</p:TypeCode>
                </p:Packaging>
                <p:ValuationAdjustment>
                  <p:AdditionCode>0000</p:AdditionCode>
                </p:ValuationAdjustment>
              </p:GovernmentAgencyGoodsItem>
              <p:Importer>
                <p:ID>GB519594455000</p:ID>
                <p:Address/>
              </p:Importer>
              <p:PreviousDocument>
                <p:SequenceNumeric>2</p:SequenceNumeric>
                <p:CategoryCode>Y</p:CategoryCode>
                <p:ID>12345</p:ID>
                <p:TypeCode>DCR</p:TypeCode>
                <p:LineNumeric>1</p:LineNumeric>
              </p:PreviousDocument>
              <p:PreviousDocument>
                <p:SequenceNumeric>1</p:SequenceNumeric>
                <p:CategoryCode>Y</p:CategoryCode>
                <p:ID>201710017INSERTEORI-12345</p:ID>
                <p:TypeCode>DCR</p:TypeCode>
                <p:LineNumeric>1</p:LineNumeric>
              </p:PreviousDocument>
              <p:TradeTerms>
                <p:ConditionCode>DDP</p:ConditionCode>
                <p:LocationID>GBTIL</p:LocationID>
              </p:TradeTerms>
              <p:UCR>
                <p:TraderAssignedReferenceID>7INSERTEORI-12345</p:TraderAssignedReferenceID>
              </p:UCR>
              <p:Warehouse>
                <p:ID>8887776668</p:ID>
                <p:TypeCode>U</p:TypeCode>
              </p:Warehouse>
            </p:GoodsShipment>
            <p:ObligationGuarantee>
              <p:SequenceNumeric>1</p:SequenceNumeric>
              <p:ID>7121503</p:ID>
              <p:SecurityDetailsCode>0</p:SecurityDetailsCode>
            </p:ObligationGuarantee>
            <p:SupervisingOffice>
              <p:ID>GBBEL004</p:ID>
            </p:SupervisingOffice>
          </p:Declaration>
        </p:FullDeclarationObject>
      </p:FullDeclarationDataDetails>
    </p:DeclarationFullResponse>
  }
}
