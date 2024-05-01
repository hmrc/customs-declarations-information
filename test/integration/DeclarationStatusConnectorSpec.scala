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

package integration

import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationStatusConnector
import uk.gov.hmrc.customs.declarations.information.model.{AuthorisedRequest, Csp, VersionOne}
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV1
import util.ExternalServicesConfig.{AuthToken, Host, Port}
import util.StatusTestXMLData.expectedStatusPayloadRequest
import util.TestData._
import util._
import util.externalservices.BackendDeclarationService

class DeclarationStatusConnectorSpec extends IntegrationTestSpec
  with GuiceOneAppPerSuite

  with BackendDeclarationService {

  private lazy val connector = app.injector.instanceOf[DeclarationStatusConnector]

  private val incomingAuthToken = s"Bearer ${ExternalServicesConfig.AuthToken}"
  private implicit val asr: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, VersionOne,
    ApiSubscriptionFieldsTestData.clientId, None, None, None, Csp(Some(declarantEori), Some(badgeIdentifier)), mock(classOf[Request[AnyContent]]))

  override protected def beforeAll(): Unit = {
    startMockServer()
  }

  override protected def beforeEach(): Unit = {
    when(mockUuidService.uuid()).thenReturn(correlationIdUuid)
  }

  override protected def afterEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll(): Unit = {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(TestModule.asGuiceableModule)).configure(Map(
      "microservice.services.declaration-status.host" -> Host,
      "microservice.services.declaration-status.port" -> Port,
      "microservice.services.declaration-status.context" -> BackendStatusDeclarationServiceContextV1,
      "microservice.services.declaration-status.bearer-token" -> AuthToken,
      "internalServiceHostPatterns" -> List("^.*\\.service$", "^.*\\.mdtp$"),
      "metrics.enabled" -> false
    )).build()

  "DeclarationStatusConnector" should {

    "make a correct request for a CSP" in {
      startBackendStatusServiceV1()
      await(sendValidXml())
      verifyBackendDecServiceWasCalledWith(requestBody = expectedStatusPayloadRequest.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }

    "make a correct request for a non-CSP" in {
      startBackendStatusServiceV1()
      await(sendValidXml())
      verifyBackendDecServiceWasCalledWith(requestBody = expectedStatusPayloadRequest.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }
  }

  private def sendValidXml() = {
    connector.send(date, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), mrn)
  }
}
