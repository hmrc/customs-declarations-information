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
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationFullConnector
import uk.gov.hmrc.customs.declarations.information.model.{AuthorisedRequest, ConnectionError, Csp, UnexpectedError, VersionOne}
import uk.gov.hmrc.http.HttpResponse
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.CustomsDeclarationsExternalServicesConfig.BackendFullDeclarationServiceContextV1
import util.ExternalServicesConfig.{AuthToken, Host, Port}
import util.FullTestXMLData.expectedFullPayloadRequest
import util.TestData._
import util._
import util.externalservices.BackendDeclarationService

import scala.concurrent.Future

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

    "return an error when fail to connect the external service" in {
      stopMockServer()
      val response = await(sendValidXml())
      inside(response) { case Left(UnexpectedError(_)) => succeed }
      startMockServer()
    }
  }

  private def sendValidXml(): Future[Either[ConnectionError, HttpResponse]] = {
    connector.send(date, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), mrn)
  }
}
