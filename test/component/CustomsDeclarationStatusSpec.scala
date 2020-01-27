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
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, BackendStatusDeclarationService}
import util.{AuditService, CustomsDeclarationsExternalServicesConfig, StatusTestXMLData, TestData}

import scala.concurrent.Future

class CustomsDeclarationStatusSpec extends ComponentTestSpec
  with AuditService
  with ExpectedTestResponses
  with Matchers
  with OptionValues
  with BackendStatusDeclarationService
  with ApiSubscriptionFieldsService
  with AuthService {

  private val mrn = "some-mrn"
  private val ducr = "some-ducr"
  private val ucr = "some-ucr"
  private val inventoryReference = "some-ir"

  private val endpointMRN = s"/mrn/$mrn/status"
  private val endpointDUCR = s"/ducr/$ducr/status"
  private val endpointUCR = s"/ucr/$ucr/status"
  private val endpointIR = s"/inventory-reference/$inventoryReference/status"

  private val apiSubscriptionKeyForXClientId =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations-information", version = VersionOne)

  private def validResponse() =
    raw"""<p:DeclarationStatusResponse 
      |xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:p4="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6" xmlns:p3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:p2="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:p1="urn:wco:datamodel:WCO:Response_DS:DMS:2" xmlns:p="http://gov.uk/customs/declarationInformationRetrieval/status/v2">
      |  <p:DeclarationStatusDetails>
      |    <p:Declaration>
      |      <p:AcceptanceDateTime>
      |        <p1:DateTimeString formatCode="304">20190702110757Z</p1:DateTimeString>
      |      </p:AcceptanceDateTime>
      |      <p:ID>18GB9JLC3CU1LFGVR2</p:ID>
      |      <p:VersionID>1</p:VersionID>
      |      <p:ReceivedDateTime>
      |        <p:DateTimeString formatCode="304">20190702110757Z</p:DateTimeString>
      |      </p:ReceivedDateTime>
      |      <p:GoodsReleasedDateTime>
      |        <p:DateTimeString formatCode="304">20190702110757Z</p:DateTimeString>
      |      </p:GoodsReleasedDateTime>
      |      <p:ROE>6</p:ROE>
      |      <p:ICS>15</p:ICS>
      |      <p:IRC>000</p:IRC>
      |    </p:Declaration>
      |    <p2:Declaration>
      |      <p2:FunctionCode>9</p2:FunctionCode>
      |      <p2:TypeCode>IMZ</p2:TypeCode>
      |      <p2:GoodsItemQuantity>100</p2:GoodsItemQuantity>
      |      <p2:TotalPackageQuantity>10</p2:TotalPackageQuantity>
      |      <p2:Submitter>
      |        <p2:ID>GB123456789012000</p2:ID>
      |      </p2:Submitter>
      |      <p2:GoodsShipment>
      |        <p2:PreviousDocument>
      |          <p2:ID>18GBAKZ81EQJ2FGVR</p2:ID>
      |          <p2:TypeCode>DCR</p2:TypeCode>
      |        </p2:PreviousDocument>
      |        <p2:PreviousDocument>
      |          <p2:ID>18GBAKZ81EQJ2FGVA</p2:ID>
      |          <p2:TypeCode>MCR</p2:TypeCode>
      |        </p2:PreviousDocument>
      |        <p2:UCR>
      |          <p2:TraderAssignedReferenceID>20GBAKZ81EQJ2WXYZ</p2:TraderAssignedReferenceID>
      |        </p2:UCR>
      |      </p2:GoodsShipment>
      |    </p2:Declaration>
      |  </p:DeclarationStatusDetails>
      |</p:DeclarationStatusResponse>
      |""".stripMargin

  private def createFakeRequest(endpoint: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", endpoint).withHeaders(ValidHeaders.-(CONTENT_TYPE).toSeq: _*)

  val validCspRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointMRN).fromCsp
  val validNonCspRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointMRN).fromNonCsp
  val validMrnRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointMRN).fromCsp
  val validDucrRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointDUCR).fromCsp
  val validUcrRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointUCR).fromCsp
  val validIrRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointIR).fromCsp

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
      startBackendStatusService()
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
      startBackendStatusService()

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

  feature("Declaration Information API query declaration statuses") {
    scenario("An authorised CSP successfully queries declaration status by an MRN value") {
      Given("A CSP wants the status of a declaration with a given MRN")
      startBackendStatusService()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a MRN request with data is sent to the API")
      val result: Future[Result] = route(app = app, validMrnRequest).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      And("the response body is a valid status xml")
      contentAsString(result) shouldBe validResponse

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCsp())

      And("v1 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContext))))
    }

    scenario("An authorised CSP unsuccessfully queries declaration status by an DUCR value") {
      Given("A CSP wants the status of a declaration associated with a given DUCR")
      startBackendStatusService()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a MRN request with data is sent to the API")
      val result: Future[Result] = route(app = app, validDucrRequest).value

      println(contentAsString(result))

      Then("a response with a 400 (BAD_REQUEST) status is received")
      status(result) shouldBe BAD_REQUEST

      And("the response body is a valid status xml")
      contentAsString(result) shouldBe validResponse

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCsp())

      And("v1 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContext))))
    }
  }
}
