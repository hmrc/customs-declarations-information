/*
 * Copyright 2019 HM Revenue & Customs
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
import util.XmlOps.stringToXml
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, MdgStatusDeclarationService}
import util.{AuditService, CustomsDeclarationsExternalServicesConfig, StatusTestXMLData}

import scala.concurrent.Future

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

  private def validResponse(acceptanceDateVal : String) =
    raw"""<v1:DeclarationStatusResponse 
      |xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v1 ../Schemas/declarationInformationRetrievalStatusResponse.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:_3="urn:wco:datamodel:WCO:Response_DS:DMS:2" xmlns:_2="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:v1="http://gov.uk/customs/declarationInformationRetrieval/status/v1">
      |  <v1:Declaration>
      |    <v1:AcceptanceDateTime>
      |      <_3:DateTimeString>$acceptanceDateVal</_3:DateTimeString>
      |    </v1:AcceptanceDateTime>
      |    <v1:VersionID>0</v1:VersionID>
      |    <v1:ID>mrn</v1:ID>
      |    <v1:CreationDateTime>
      |      <v1:DateTimeString formatCode="string">2001-12-17T09:30:47Z</v1:DateTimeString>
      |    </v1:CreationDateTime>
      |  </v1:Declaration>
      |  <_2:Declaration>
      |    <_2:FunctionCode>9</_2:FunctionCode>
      |    <_2:TypeCode>IM4567declaration type</_2:TypeCode>
      |    <_2:GoodsItemQuantity unitType="101">2</_2:GoodsItemQuantity>
      |    <_2:TotalPackageQuantity>3</_2:TotalPackageQuantity>
      |    <_2:Submitter>
      |      <_2:ID>123456</_2:ID>
      |    </_2:Submitter>
      |  </_2:Declaration>
      |</v1:DeclarationStatusResponse>
      |""".stripMargin

  val validRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", endpoint).withHeaders(ValidHeaders.-(CONTENT_TYPE).toSeq: _*).fromCsp

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("Declaration API authorises status requests from CSPs with v1.0 accept header") {
    scenario("An authorised CSP successfully requests a status") {
      Given("A CSP wants the status of a declaration")
      startMdgStatusService()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSPNoNrs()

      When("a GET request with data is sent to the API")
      val result: Future[Result] = route(app = app, validRequest).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      And("the response body is a valid status xml")
      contentAsString(result) shouldBe validResponse(acceptanceDateVal.toString(ISO_UTC_DateTimeFormat))

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCspNoNrs())

      And("v1 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgStatusDeclarationServiceContext))))
    }
  }

  feature("Declaration API handles status request errors from CSPs as expected") {

    scenario("Response status 400 when Date of declaration is older than configured allowed value") {
      Given("the API is available")
      startMdgStatusService(body = StatusTestXMLData.generateDeclarationStatusResponse(acceptanceOrCreationDate = DateTime.now().minusYears(1)))
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSPNoNrs()

      When("a GET request with data is sent to the API")
      val result: Future[Result] = route(app = app, validRequest).value

      Then(s"a response with a 400 status is received")
      status(result) shouldBe BAD_REQUEST

      And("the response body is a \"invalid xml\" XML")
      stringToXml(contentAsString(result)) shouldBe stringToXml(BadStatusResponseErrorInvalidDate)
    }

    scenario("Response status 400 when Declaration Management Information Response does not contain a valid communicationAddress") {
      Given("the API is available")
      startMdgStatusService(body = StatusTestXMLData.statusResponseDeclarationNoCommunicationAddress)
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSPNoNrs()

      When("a GET request with data is sent to the API")
      val result: Future[Result] = route(app = app, validRequest).value

      Then(s"a response with a 400 status is received")
      status(result) shouldBe BAD_REQUEST

      And("the response body is a \"invalid xml\" XML")
      stringToXml(contentAsString(result)) shouldBe stringToXml(BadStatusResponseErrorBadgeIdMissingOrInvalid)
    }

    scenario("Response status 400 when Declaration Management Information Response does contains different Badge Identifier") {
      Given("the API is available")
      startMdgStatusService(body = StatusTestXMLData.generateDeclarationStatusResponse(communicationAddress = "hmrcgwid:144b80b0-b46e-4c56-be1a-83b36649ac46:ad3a8c50-fc1c-4b81-a56cbb153aced791:IWONTMATCH"))
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientId)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSPNoNrs()

      When("a GET request with data is sent to the API")
      val result: Future[Result] = route(app = app, validRequest).value

      Then(s"a response with a 400 status is received")
      status(result) shouldBe BAD_REQUEST

      And("the response body is a \"invalid xml\" XML")
      stringToXml(contentAsString(result)) shouldBe stringToXml(BadStatusResponseErrorBadgeIdMissingOrInvalid)
    }
  }

}
