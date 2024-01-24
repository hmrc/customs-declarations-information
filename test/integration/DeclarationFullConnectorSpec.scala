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
import org.scalatest.Inside
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationConnector._
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationFullConnector
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.customs.declarations.information.model.{Csp, VersionOne}
import uk.gov.hmrc.http._
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.CustomsDeclarationsExternalServicesConfig.BackendFullDeclarationServiceContextV1
import util.ExternalServicesConfig.{AuthToken, Host, Port}
import util.FullTestXMLData.expectedFullPayloadRequest
import util.TestData._
import util._
import util.externalservices.BackendDeclarationService

class DeclarationFullConnectorSpec extends IntegrationTestSpec
  with GuiceOneAppPerSuite
  with Inside
  with BackendDeclarationService {

  private lazy val connector = app.injector.instanceOf[DeclarationFullConnector]

  private val incomingAuthToken = s"Bearer ${ExternalServicesConfig.AuthToken}"
  private implicit val asr: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, VersionOne,
    ApiSubscriptionFieldsTestData.clientId, None, None, Some(1), Csp(Some(declarantEori), Some(badgeIdentifier)), mock(classOf[Request[AnyContent]]))

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
      "microservice.services.declaration-full.host" -> Host,
      "microservice.services.declaration-full.port" -> Port,
      "microservice.services.declaration-full.context" -> BackendFullDeclarationServiceContextV1,
      "microservice.services.declaration-full.bearer-token" -> AuthToken,
      "internalServiceHostPatterns" -> List("^.*\\.service$", "^.*\\.mdtp$"),
      "metrics.enabled" -> false
    )).build()

  "DeclarationFullConnector" should {

    "make a correct request for a CSP" in {
      startBackendFullServiceV1()
      await(sendValidXml())
      verifyBackendDecServiceWasCalledWith(BackendFullDeclarationServiceContextV1, requestBody = expectedFullPayloadRequest.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }

    "make a correct request for a non-CSP" in {
      startBackendFullServiceV1()
      await(sendValidXml())
      verifyBackendDecServiceWasCalledWith(BackendFullDeclarationServiceContextV1, requestBody = expectedFullPayloadRequest.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }

    "return a failed future when external service returns 500" in {
      startBackendFullServiceV1(INTERNAL_SERVER_ERROR)
      inside(await(sendValidXml())) { case Left(Non2xxResponseError(status, _)) =>
        status shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()
      intercept[BadGatewayException](await(sendValidXml())).responseCode shouldBe BAD_GATEWAY
      startMockServer()
    }
  }

  private def sendValidXml() = {
    connector.send(date, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), mrn)
  }
}
