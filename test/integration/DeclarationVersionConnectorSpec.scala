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

package integration

import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declarations.information.connectors.{DeclarationVersionConnector, Non2xxResponseException}
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.customs.declarations.information.model.{Csp, VersionOne}
import uk.gov.hmrc.http._
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.CustomsDeclarationsExternalServicesConfig.BackendVersionDeclarationServiceContextV1
import util.ExternalServicesConfig.{AuthToken, Host, Port}
import util.TestData._
import util.VersionTestXMLData.expectedVersionPayloadRequest
import util._
import util.externalservices.BackendDeclarationService

class DeclarationVersionConnectorSpec extends IntegrationTestSpec
  with GuiceOneAppPerSuite
  with MockitoSugar
  with BackendDeclarationService {

  private lazy val connector = app.injector.instanceOf[DeclarationVersionConnector]

  private val incomingAuthToken = s"Bearer ${ExternalServicesConfig.AuthToken}"
  private implicit val asr: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, VersionOne,
    ApiSubscriptionFieldsTestData.clientId, None, Csp(Some(declarantEori), Some(badgeIdentifier)), mock[Request[AnyContent]])

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    when(mockUuidService.uuid()).thenReturn(correlationIdUuid)
  }

  override protected def afterEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(TestModule.asGuiceableModule)).configure(Map(
      "microservice.services.declaration-version.host" -> Host,
      "microservice.services.declaration-version.port" -> Port,
      "microservice.services.declaration-version.context" -> BackendVersionDeclarationServiceContextV1,
      "microservice.services.declaration-version.bearer-token" -> AuthToken,
      "internalServiceHostPatterns" -> List("^.*\\.service$", "^.*\\.mdtp$")
    )).build()

  "DeclarationVersionConnector" should {

    "make a correct request for a CSP" in {
      startBackendVersionServiceV1()
      await(sendValidXml())
      verifyBackendDecServiceWasCalledWith(BackendVersionDeclarationServiceContextV1, requestBody = expectedVersionPayloadRequest.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }

    "make a correct request for a non-CSP" in {
      startBackendVersionServiceV1()
      await(sendValidXml())
      verifyBackendDecServiceWasCalledWith(BackendVersionDeclarationServiceContextV1, requestBody = expectedVersionPayloadRequest.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }

    "return a failed future when external service returns 404" in {
      startBackendVersionServiceV1(NOT_FOUND)
      intercept[Non2xxResponseException](await(sendValidXml())).responseCode shouldBe NOT_FOUND
    }

    "return a failed future when external service returns 400" in {
      startBackendVersionServiceV1(BAD_REQUEST)
      intercept[Non2xxResponseException](await(sendValidXml())).responseCode shouldBe BAD_REQUEST
    }

    "return a failed future when external service returns 500" in {
      startBackendVersionServiceV1(INTERNAL_SERVER_ERROR)
      intercept[Non2xxResponseException](await(sendValidXml())).responseCode shouldBe INTERNAL_SERVER_ERROR
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()
      intercept[BadGatewayException](await(sendValidXml())).responseCode shouldBe BAD_GATEWAY
      startMockServer()
    }
  }

  private def sendValidXml() = {
    connector.send(date, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), Right(mrn))
  }
}
