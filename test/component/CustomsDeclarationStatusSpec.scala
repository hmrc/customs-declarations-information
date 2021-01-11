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

package component

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo, verify}
import org.scalatest._
import play.api.Application
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.customs.declarations.information.model.{ApiSubscriptionKey, VersionOne}
import util.FakeRequests.FakeRequestOps
import util.RequestHeaders.{ACCEPT_HMRC_XML_HEADER_V2, ValidHeaders}
import util.TestData.nonCspBearerToken
import util.XmlOps.stringToXml
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, BackendStatusDeclarationService}
import util.{AuditService, CustomsDeclarationsExternalServicesConfig}

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
  private val inventoryReference = "some%2F-ir"

  private val endpointMRN = s"/mrn/$mrn/status"
  private val endpointMissingMRN = s"/mrn//status"
  private val endpointDUCR = s"/ducr/$ducr/status"
  private val endpointMissingDUCR = s"/ducr//status"
  private val endpointUCR = s"/ucr/$ucr/status"
  private val endpointMissingUCR = s"/ucr//status"
  private val endpointIR = s"/inventory-reference/$inventoryReference/status"
  private val endpointMissingIR = s"/inventory-reference//status"

  private val apiSubscriptionKeyForXClientId =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations-information", version = VersionOne)

  private def validResponse() =
    """<p:DeclarationStatusResponse xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:p4="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6" xmlns:p3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:p2="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:p1="urn:wco:datamodel:WCO:Response_DS:DMS:2" xmlns:p="http://gov.uk/customs/declarationInformationRetrieval/status/v2">
      |      <p:DeclarationStatusDetails>
      |          <p:Declaration>
      |                <p:AcceptanceDateTime>
      |                  <p1:DateTimeString formatCode="304">20190702110757Z</p1:DateTimeString>
      |                </p:AcceptanceDateTime>
      |                <p:ID>18GB9JLC3CU1LFGVR2</p:ID>
      |                <p:VersionID>1</p:VersionID>
      |                <p:ReceivedDateTime>
      |                  <p:DateTimeString formatCode="304">20190702110757Z</p:DateTimeString>
      |                </p:ReceivedDateTime>
      |                <p:GoodsReleasedDateTime>
      |                  <p:DateTimeString formatCode="304">20190702110757Z</p:DateTimeString>
      |                </p:GoodsReleasedDateTime>
      |                <p:ROE>6</p:ROE>
      |                <p:ICS>15</p:ICS>
      |                <p:IRC>000</p:IRC>
      |              </p:Declaration>
      |          <p2:Declaration>
      |                <p2:FunctionCode>9</p2:FunctionCode>
      |                <p2:TypeCode>IMZ</p2:TypeCode>
      |                <p2:GoodsItemQuantity>100</p2:GoodsItemQuantity>
      |                <p2:TotalPackageQuantity>10</p2:TotalPackageQuantity>
      |                <p2:Submitter>
      |                  <p2:ID>GB123456789012000</p2:ID>
      |                </p2:Submitter>
      |                <p2:GoodsShipment>
      |                  <p2:PreviousDocument>
      |                    <p2:ID>18GBAKZ81EQJ2FGVR</p2:ID>
      |                    <p2:TypeCode>DCR</p2:TypeCode>
      |                  </p2:PreviousDocument>
      |                  <p2:PreviousDocument>
      |                    <p2:ID>18GBAKZ81EQJ2FGVA</p2:ID>
      |                    <p2:TypeCode>MCR</p2:TypeCode>
      |                  </p2:PreviousDocument>
      |                  <p2:UCR>
      |                    <p2:TraderAssignedReferenceID>20GBAKZ81EQJ2WXYZ</p2:TraderAssignedReferenceID>
      |                  </p2:UCR>
      |                </p2:GoodsShipment>
      |              </p2:Declaration>
      |        </p:DeclarationStatusDetails>
      |    </p:DeclarationStatusResponse>""".stripMargin

  val notImplementedResponse =
    """<?xml version='1.0' encoding='UTF-8'?>
         |<errorResponse>
         |      <code>NOT_IMPLEMENTED</code>
         |      <message>Not yet available</message>
         |      
         |    </errorResponse>""".stripMargin

  val invalidSearchResponse =
    """<?xml version='1.0' encoding='UTF-8'?>
         |<errorResponse>
         |      <code>NOT_FOUND</code>
         |      <message>Invalid Search</message>
         |      
         |    </errorResponse>""".stripMargin

  val missingSearchResponse =
    """<?xml version='1.0' encoding='UTF-8'?>
         |<errorResponse>
         |      <code>BAD_REQUEST</code>
         |      <message>Missing search parameter</message>
         |
         |    </errorResponse>""".stripMargin


  private def createFakeRequest(endpoint: String, headers: Map[String, String] = ValidHeaders): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", endpoint).withHeaders(headers.-(CONTENT_TYPE).toSeq: _*)

  val validCspRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointMRN).fromCsp
  val validNonCspRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointMRN).fromNonCsp
  val validNonCspRequestV2: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpoint = endpointMRN, headers = ValidHeaders + ACCEPT_HMRC_XML_HEADER_V2).fromNonCsp
  val validMrnRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointMRN).fromCsp
  val missingMrnRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointMissingMRN).fromCsp
  val validDucrRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointDUCR).fromCsp
  val missingDucrRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointMissingDUCR).fromCsp
  val validUcrRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointUCR).fromCsp
  val missingUcrRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointMissingUCR).fromCsp
  val validIrRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointIR).fromCsp
  val missingIrRequest: FakeRequest[AnyContentAsEmpty.type] = createFakeRequest(endpointMissingIR).fromCsp

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("Declaration Information API returns unavailable when a version is shuttered") {
    scenario("An authorised CSP fails to submit a customs declaration information request to a shuttered version") {
      Given("A CSP wants to submit a customs declaration information request to a shuttered version")

      implicit lazy val app: Application = super.app(configMap + ("shutter.v1" -> "true"))

      startBackendStatusServiceV1()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a request with data is sent to the API")
      val result: Future[Result] = route(app = app, validCspRequest).value

      Then("a response with a 503 (SERVICE_UNAVAILABLE) status is received")
      status(result) shouldBe SERVICE_UNAVAILABLE

      And("the response body is empty")
      stringToXml(contentAsString(result)) shouldBe stringToXml(ServiceUnavailableError)
      }
    }

  feature("Declaration Information API authorises status requests from CSPs with v1.0 accept header") {
    scenario("An authorised CSP successfully requests a status") {
      Given("A CSP wants the status of a declaration")
      startBackendStatusServiceV1()
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
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV1))))
    }
  }

  feature("Declaration Information API authorises status requests from non-CSPs with v1.0 accept header") {
    scenario("An authorised non-CSP successfully requests a status") {
      Given("A non-CSP wants the status of a declaration")
      startBackendStatusServiceV1()

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
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV1))))
    }
  }
  
  feature("Declaration Information API authorises status requests from non-CSPs with v2.0 accept header") {
    scenario("An authorised non-CSP successfully requests a status") {
      Given("A non-CSP wants the status of a declaration")
      startBackendStatusServiceV2()

      And("the non-CSP is authorised with its privileged application")
      authServiceUnauthorisesScopeForCSPWithoutRetrievals(nonCspBearerToken)
      authServiceAuthorizesNonCspWithEori()

      When("a GET request with data is sent to the API")
      val result: Future[Result] = route(app = app, validNonCspRequestV2).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      And("the response body is a valid status xml")
      contentAsString(result) shouldBe validResponse

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())

      And("v2 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV2))))
    }
  }

  feature("Declaration Information API query declaration statuses by MRN") {
    scenario("An authorised CSP successfully queries declaration status by an MRN value") {
      Given("A CSP wants the status of a declaration with a given MRN")
      startBackendStatusServiceV1()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a MRN request with data is sent to the API")
      val result: Future[Result] = route(app = app, validMrnRequest).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      And("the response body is a valid status xml")
      contentAsString(result) shouldBe validResponse()

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCsp())

      And("v1 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV1))))
    }
    
    scenario("An authorised CSP queries declaration status with missing MRN value") {
      Given("A CSP omits the MRN")
      startBackendStatusServiceV1()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a MRN request with data is sent to the API")
      val result: Future[Result] = route(app = app, missingMrnRequest).value

      Then("a response with a 400 status is received")
      status(result) shouldBe BAD_REQUEST

      And("the response body is a valid status xml")
      stringToXml(contentAsString(result)) shouldBe stringToXml(missingSearchResponse)
    }
  }

  feature("Declaration Information API query declaration statuses by DUCR") {
    scenario("An authorised CSP successfully queries declaration status by an DUCR value") {
      Given("A CSP wants the status of a declaration associated with a given DUCR")
      startBackendStatusServiceV1()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a DUCR request with data is sent to the API")
      val result: Future[Result] = route(app = app, validDucrRequest).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      And("the response body is a valid status xml")
      contentAsString(result) shouldBe validResponse()

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCsp())

      And("v1 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV1))))
    }

    scenario("An authorised CSP queries declaration status with missing DUCR value") {
      Given("A CSP omits the DUCR")
      startBackendStatusServiceV1()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a DUCR request with data is sent to the API")
      val result: Future[Result] = route(app = app, missingDucrRequest).value

      Then("a response with a 400 status is received")
      status(result) shouldBe BAD_REQUEST

      And("the response body is a valid status xml")
      stringToXml(contentAsString(result)) shouldBe stringToXml(missingSearchResponse)
    }
  }

  feature("Declaration Information API query declaration statuses by UCR") {
    scenario("An authorised CSP unsuccessfully queries declaration status by an UCR value") {
      Given("A CSP wants the status of a declaration associated with a given UCR")
      startBackendStatusServiceV1()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a UCR request with data is sent to the API")
      val result: Future[Result] = route(app = app, validUcrRequest).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      And("the response body is a valid status xml")
      contentAsString(result) shouldBe validResponse()

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCsp())

      And("v1 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV1))))
    }

    scenario("An authorised CSP queries declaration status with missing UCR value") {
      Given("A CSP omits the UCR")
      startBackendStatusServiceV1()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a UCR request with data is sent to the API")
      val result: Future[Result] = route(app = app, missingUcrRequest).value

      Then("a response with a 400 status is received")
      status(result) shouldBe BAD_REQUEST

      And("the response body is a valid status xml")
      stringToXml(contentAsString(result)) shouldBe stringToXml(missingSearchResponse)
    }
  }

  feature("Declaration Information API query declaration statuses by Inventory Reference") {
    scenario("An authorised CSP unsuccessfully queries declaration status by an Inventory Reference value") {
      Given("A CSP wants the status of a declaration associated with a given Inventory Reference")
      startBackendStatusServiceV1()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a Inventory Reference request with data is sent to the API")
      val result: Future[Result] = route(app = app, validIrRequest).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      And("the response body is a valid status xml")
      contentAsString(result) shouldBe validResponse()

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCsp())

      And("v1 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV1))))
    }

    scenario("An authorised CSP queries declaration status with missing Inventory Reference value") {
      Given("A CSP omits the Inventory Reference")
      startBackendStatusServiceV1()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a UCR request with data is sent to the API")
      val result: Future[Result] = route(app = app, missingIrRequest).value

      Then("a response with a 400 status is received")
      status(result) shouldBe BAD_REQUEST

      And("the response body is a valid status xml")
      stringToXml(contentAsString(result)) shouldBe stringToXml(missingSearchResponse)
    }
  }
}
