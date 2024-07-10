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

package unit.connectors

import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.config.{ConfigService, InformationCircuitBreakerConfig}
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationStatusConnector
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.xml.BackendStatusPayloadCreator
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.StatusTestXMLData.expectedStatusPayloadRequest
import util.TestData._
import util.{ApiSubscriptionFieldsTestData, UnitSpec}

import scala.concurrent.{ExecutionContext, Future}

class DeclarationStatusConnectorSpec extends UnitSpec with BeforeAndAfterEach with Eventually {
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val mockWsPost = mock(classOf[HttpClient])
  private val mockLogger = stubInformationLogger
  private val mockServiceConfigProvider = mock(classOf[ServiceConfigProvider])
  private val mockInformationConfigService = mock(classOf[ConfigService])
  private val mockBackendPayloadCreator = mock(classOf[BackendStatusPayloadCreator])
  private val informationCircuitBreakerConfig: InformationCircuitBreakerConfig = InformationCircuitBreakerConfig(50, 1000, 10000)
  private val actorSystem = ActorSystem("mockActorSystem")
  private val connector = new DeclarationStatusConnector(mockWsPost, mockLogger, mockBackendPayloadCreator, mockServiceConfigProvider, mockInformationConfigService, mock(classOf[CdsLogger]), actorSystem)
  private val v1Config: ServiceConfig = ServiceConfig("v1-url", Some("v1-bearer"), "v1-default")
  private implicit val asr: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, VersionOne, ApiSubscriptionFieldsTestData.clientId, None, None, None, Csp(Some(declarantEori), Some(badgeIdentifier)), mock(classOf[Request[AnyContent]]))

  override protected def beforeEach(): Unit = {
    reset(mockWsPost, mockServiceConfigProvider)
    when(mockServiceConfigProvider.getConfig("declaration-status")).thenReturn(v1Config)
    when(mockInformationConfigService.informationCircuitBreakerConfig).thenReturn(informationCircuitBreakerConfig)
    when(mockBackendPayloadCreator.create(conversationId, correlationId, date, mrn, Some(apiSubscriptionFieldsResponse))(asr)).thenReturn(expectedStatusPayloadRequest)
  }

  private val successfulResponse = HttpResponse(200, "")

  "DeclarationStatusConnector" when {

    "making a successful request" should {

      "pass URL from config" in {
        returnResponseForRequest(Future.successful(successfulResponse))

        awaitRequest

        verify(mockWsPost).POSTString(ameq(v1Config.url), anyString, any[Seq[(String, String)]])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "pass in the body" in {
        returnResponseForRequest(Future.successful(successfulResponse))

        awaitRequest

        verify(mockWsPost).POSTString(anyString, ameq(expectedStatusPayloadRequest.toString()), any[Seq[(String, String)]])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "prefix the config key with the prefix if passed" in {
        returnResponseForRequest(Future.successful(successfulResponse))

        awaitRequest

        verify(mockServiceConfigProvider).getConfig("declaration-status")
      }
    }

    "configuration is absent" should {
      "throw an exception when no config is found for given api and version combination" in {
        when(mockServiceConfigProvider.getConfig("declaration-status")).thenReturn(null)

        val caught = intercept[IllegalArgumentException] {
          await(connector.send(date, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), mrn))
        }
        caught.getMessage shouldBe "config not found"
      }
    }
  }

  private def awaitRequest: Either[ConnectionError, HttpResponse] = {
    await(connector.send(date, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), mrn))
  }

  private def returnResponseForRequest(eventualResponse: Future[HttpResponse]): OngoingStubbing[Future[HttpResponse]] = {
    when(mockWsPost.POSTString(anyString, anyString, any[Seq[(String, String)]])(
      any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext]))
      .thenReturn(eventualResponse)
  }
}
