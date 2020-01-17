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

package component

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo, verify}
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import org.scalatest._
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.customs.declarations.information.model.{ApiSubscriptionKey, VersionOne}
import util.FakeRequests.FakeRequestOps
import util.RequestHeaders.ValidHeaders
import util.TestData.nonCspBearerToken
import util.XmlOps.stringToXml
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, MdgStatusDeclarationService}
import util.{AuditService, CustomsDeclarationsExternalServicesConfig, StatusTestXMLData, TestData}

import scala.concurrent.Future

//TODO requires correct xml transform implementation
@Ignore
class CustomsDeclarationStatusSpec extends ComponentTestSpec
  with AuditService
  with ExpectedTestResponses
  with Matchers
  with OptionValues
  with MdgStatusDeclarationService
  with ApiSubscriptionFieldsService
  with AuthService {

  private val mrn = "some-mrn"
  private val endpoint = s"/mrn/$mrn/status"

  private val ISO_UTC_DateTimeFormat: DateTimeFormatter = ISODateTimeFormat.dateTime.withZoneUTC()

  private val apiSubscriptionKeyForXClientId =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations-information", version = VersionOne)

  //TODO should be the transformed response
  private def validResponse() =
    raw"""<n1:queryDeclarationStatusResponse 
         |        xmlns:od="urn:wco:datamodel:WCO:DEC-DMS:2" 
         |        xmlns:otnds="urn:wco:datamodel:WCO:Response_DS:DMS:2" 
         |        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
         |        xmlns:ds="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" 
         |        xmlns:n1="http://gov.uk/customs/declarationInformationRetrieval/status/v2" 
         |        xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v2 queryDeclarationStatusResponse.xsd">
         |    <n1:responseCommon>
         |        <n1:processingDate>2001-12-17T09:30:47Z</n1:processingDate>
         |    </n1:responseCommon>
         |    <n1:responseDetail>
         |        <n1:retrieveDeclarationStatusResponse>
         |            <n1:retrieveDeclarationStatusDetailsList>
         |                <n1:retrieveDeclarationStatusDetails>
         |                    <n1:Declaration>
         |                        <n1:AcceptanceDateTime>
         |                            <otnds:DateTimeString formatCode="304">20190702110757Z</otnds:DateTimeString>
         |                        </n1:AcceptanceDateTime>
         |                        <n1:ID>18GB9JLC3CU1LFGVR2</n1:ID>
         |                        <n1:VersionID>1</n1:VersionID>
         |                        <n1:ReceivedDateTime>
         |                            <n1:DateTimeString formatCode="304">20190702110757Z</n1:DateTimeString>
         |                        </n1:ReceivedDateTime>
         |                        <n1:GoodsReleasedDateTime>
         |                            <n1:DateTimeString formatCode="304">20190702110757Z</n1:DateTimeString>
         |                        </n1:GoodsReleasedDateTime>
         |                        <n1:ROE>6</n1:ROE>
         |                        <n1:ICS>15</n1:ICS>
         |                        <n1:IRC>000</n1:IRC>
         |                    </n1:Declaration>
         |                    <od:Declaration>
         |                        <od:FunctionCode>9</od:FunctionCode>
         |                        <od:TypeCode>IMZ</od:TypeCode>
         |                        <od:GoodsItemQuantity unitCode="NPR">100</od:GoodsItemQuantity>
         |                        <od:TotalPackageQuantity>10</od:TotalPackageQuantity>
         |                        <od:Submitter>
         |                            <od:ID>GB123456789012000</od:ID>
         |                        </od:Submitter>
         |                        <od:GoodsShipment>
         |                            <od:PreviousDocument>
         |                                <od:ID>18GBAKZ81EQJ2FGVR</od:ID>
         |                                <od:TypeCode>DCR</od:TypeCode>
         |                            </od:PreviousDocument>
         |                            <od:PreviousDocument>
         |                                <od:ID>18GBAKZ81EQJ2FGVA</od:ID>
         |                                <od:TypeCode>MCR</od:TypeCode>
         |                            </od:PreviousDocument>
         |                            <od:PreviousDocument>
         |                                <od:ID>18GBAKZ81EQJ2FGVB</od:ID>
         |                                <od:TypeCode>MCR</od:TypeCode>
         |                            </od:PreviousDocument>
         |                            <od:PreviousDocument>
         |                                <od:ID>18GBAKZ81EQJ2FGVC</od:ID>
         |                                <od:TypeCode>DCR</od:TypeCode>
         |                            </od:PreviousDocument>
         |                            <od:PreviousDocument>
         |                                <od:ID>18GBAKZ81EQJ2FGVD</od:ID>
         |                                <od:TypeCode>MCR</od:TypeCode>
         |                            </od:PreviousDocument>
         |                            <od:PreviousDocument>
         |                                <od:ID>18GBAKZ81EQJ2FGVE</od:ID>
         |                                <od:TypeCode>MCR</od:TypeCode>
         |                            </od:PreviousDocument>
         |                            <od:UCR>
         |                                <od:TraderAssignedReferenceID>20GBAKZ81EQJ2WXYZ</od:TraderAssignedReferenceID>
         |                            </od:UCR>
         |                        </od:GoodsShipment>
         |                    </od:Declaration>
         |                </n1:retrieveDeclarationStatusDetails>
         |            </n1:retrieveDeclarationStatusDetailsList>
         |        </n1:retrieveDeclarationStatusResponse>
         |    </n1:responseDetail>
         |</n1:queryDeclarationStatusResponse>""".stripMargin

  val validCspRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", endpoint).withHeaders(ValidHeaders.-(CONTENT_TYPE).toSeq: _*).fromCsp
  val validNonCspRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", endpoint).withHeaders(ValidHeaders.-(CONTENT_TYPE).toSeq: _*).fromNonCsp

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("Declaration Information API authorises status requests from CSPs with v1.0 accept header") {
    scenario("An authorised CSP successfully requests a status") {
      Given("A CSP wants the status of a declaration")
      startMdgStatusService()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a GET request with data is sent to the API")
      val result: Future[Result] = route(app = app, validCspRequest).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      And("the response body is a valid status xml")
      contentAsString(result) shouldBe validResponse

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCsp())

      And("v1 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContext))))
    }
  }

  feature("Declaration Information API authorises status requests from non-CSPs with v1.0 accept header") {
    scenario("An authorised non-CSP successfully requests a status") {
      Given("A non-CSP wants the status of a declaration")
      startMdgStatusService()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the non-CSP is authorised with its privileged application")
      authServiceUnauthorisesScopeForCSPWithoutRetrievals(nonCspBearerToken)
      authServiceAuthorizesNonCspWithEori()

      When("a GET request with data is sent to the API")
      val result: Future[Result] = route(app = app, validNonCspRequest).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      And("the response body is a valid status xml")
      contentAsString(result) shouldBe validResponse

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())

      And("v1 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContext))))
    }
  }

}
