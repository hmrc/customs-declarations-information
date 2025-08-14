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
import org.scalatest.Inside.inside
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationSearchConnector
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.CustomsDeclarationsExternalServicesConfig.BackendSearchDeclarationServiceContextV1
import util.ExternalServicesConfig.{AuthToken, Host, Port}
import util.SearchTestXMLData.expectedSearchPayloadRequest
import util.TestData._
import util._
import util.externalservices.BackendDeclarationService

import scala.concurrent.Future

class DeclarationSearchConnectorSpec extends IntegrationTestSpec
  with GuiceOneAppPerSuite

  with BackendDeclarationService {

  private lazy val connector = app.injector.instanceOf[DeclarationSearchConnector]

  private val incomingAuthToken = s"Bearer ${ExternalServicesConfig.AuthToken}"
  private implicit val asr: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, VersionOne,
    ApiSubscriptionFieldsTestData.clientId, None, Some(searchParameters), None, Csp(Some(declarantEori), Some(badgeIdentifier)), mock(classOf[Request[AnyContent]]))

  override protected def beforeAll(): Unit = {
    startMockServer()
  }

  override protected def beforeEach(): Unit = {}

  override protected def afterEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll(): Unit = {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(TestModule.asGuiceableModule)).configure(Map(
      "microservice.services.declaration-search.host" -> Host,
      "microservice.services.declaration-search.port" -> Port,
      "microservice.services.declaration-search.context" -> BackendSearchDeclarationServiceContextV1,
      "microservice.services.declaration-search.bearer-token" -> AuthToken,
      "internalServiceHostPatterns" -> List("^.*\\.service$", "^.*\\.mdtp$"),
      "metrics.enabled" -> false
    )).build()

  "DeclarationSearchConnector" should {
    "make a correct request for a CSP" in {
      startBackendSearchServiceV1()
      await(sendValidXml())
      verifyBackendDecServiceWasCalledWith(BackendSearchDeclarationServiceContextV1, requestBody = expectedSearchPayloadRequest.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }

    "make a correct request for a non-CSP" in {
      startBackendSearchServiceV1()
      await(sendValidXml())
      verifyBackendDecServiceWasCalledWith(BackendSearchDeclarationServiceContextV1, requestBody = expectedSearchPayloadRequest.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()
      val response = await(sendValidXml())
      inside(response) { case Left(UnexpectedError(_)) => succeed }
      startMockServer()
    }
  }

  private def sendValidXml(): Future[Either[ConnectionError, HttpResponse]] = {
    connector.send(date, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), mrn)(asr, HeaderCarrier())
  }
}
