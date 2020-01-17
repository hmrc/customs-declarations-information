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

import uk.gov.hmrc.customs.declarations.information.model.Csp

import scala.xml.Elem

object StatusTestXMLData {

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

  val validBackendStatusResponse =
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
                    <od:PreviousDocument>
                      <od:ID>18GBAKZ81EQJ2FGVB</od:ID>
                      <od:TypeCode>MCR</od:TypeCode>
                    </od:PreviousDocument>
                    <od:PreviousDocument>
                      <od:ID>18GBAKZ81EQJ2FGVC</od:ID>
                      <od:TypeCode>DCR</od:TypeCode>
                    </od:PreviousDocument>
                    <od:PreviousDocument>
                      <od:ID>18GBAKZ81EQJ2FGVD</od:ID>
                      <od:TypeCode>MCR</od:TypeCode>
                    </od:PreviousDocument>
                    <od:PreviousDocument>
                      <od:ID>18GBAKZ81EQJ2FGVE</od:ID>
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
}
