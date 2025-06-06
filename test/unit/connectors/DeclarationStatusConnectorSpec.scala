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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, anyUrl, equalTo, postRequestedFor}
import org.apache.pekko.actor.ActorSystem
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.config.{ConfigService, InformationCircuitBreakerConfig}
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationStatusConnector
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.xml.BackendStatusPayloadCreator
import uk.gov.hmrc.http.test.{ExternalWireMockSupport, HttpClientV2Support}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.StatusTestXMLData.expectedStatusPayloadRequest
import util.TestData._
import util.{ApiSubscriptionFieldsTestData, UnitSpec}

import scala.concurrent.ExecutionContext

class DeclarationStatusConnectorSpec extends UnitSpec
  with ScalaFutures
  with HttpClientV2Support
  with IntegrationPatience
  with Eventually
  with BeforeAndAfter
  with ExternalWireMockSupport
  {
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val mockLogger = stubInformationLogger
  private val mockServiceConfigProvider = mock(classOf[ServiceConfigProvider])
  private val mockInformationConfigService = mock(classOf[ConfigService])
  private val mockBackendPayloadCreator = mock(classOf[BackendStatusPayloadCreator])
  private val informationCircuitBreakerConfig: InformationCircuitBreakerConfig = InformationCircuitBreakerConfig(50, 1000, 10000)
  private val actorSystem = ActorSystem("mockActorSystem")
  private val connector = new DeclarationStatusConnector(httpClientV2, mockLogger, mockBackendPayloadCreator, mockServiceConfigProvider, mockInformationConfigService, mock(classOf[CdsLogger]), actorSystem)
  private implicit val asr: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, VersionOne, ApiSubscriptionFieldsTestData.clientId, None, None, None, Csp(Some(declarantEori), Some(badgeIdentifier)), mock(classOf[Request[AnyContent]]))
  override protected def beforeEach(): Unit = {
    reset(mockServiceConfigProvider)
    val v1Config: ServiceConfig = ServiceConfig(externalWireMockUrl, Some("v1-bearer"), "v1-default")
    when(mockServiceConfigProvider.getConfig("declaration-status")).thenReturn(v1Config)
    when(mockInformationConfigService.informationCircuitBreakerConfig).thenReturn(informationCircuitBreakerConfig)
    when(mockBackendPayloadCreator.create(conversationId, correlationId, date, mrn, Some(apiSubscriptionFieldsResponse))(asr)).thenReturn(expectedStatusPayloadRequest)
  }


    val status = 200
    "DeclarationStatusConnector" when {

    "making a successful request" should {

      "pass URL from config" in {
        externalWireMockServer.stubFor(
          WireMock
            .post("/")
            .withHeader("X-Forwarded-Host" , equalTo("MDTP"))
            .withHeader("X-Correlation-ID" , equalTo("e61f8eee-812c-4b8f-b193-06aedc60dca2"))
            .withHeader("X-Conversation-ID" , equalTo("38400000-8cf0-11bd-b23e-10b96e4ef00d"))
            .withHeader("Date" , equalTo("Tue, 11 Sept 2018 10:28:54 UTC"))
            .withHeader("Content-Type" , equalTo("application/xml; charset=utf-8"))
            .withHeader("Accept" , equalTo("application/xml"))
            .withHeader("Authorization" , equalTo("Bearer v1-bearer"))
            .withRequestBody(equalTo(expectedStatusPayloadRequest.toString()))
            .willReturn(aResponse()
              .withBody("{}")
              .withStatus(status)
            )
        )
        awaitRequest
        externalWireMockServer.verify(postRequestedFor(anyUrl()))
      }

      "pass in the body" in {
        externalWireMockServer.stubFor(
          WireMock
            .post("/")
            .withHeader("X-Forwarded-Host" , equalTo("MDTP"))
            .withHeader("X-Correlation-ID" , equalTo("e61f8eee-812c-4b8f-b193-06aedc60dca2"))
            .withHeader("X-Conversation-ID" , equalTo("38400000-8cf0-11bd-b23e-10b96e4ef00d"))
            .withHeader("Date" , equalTo("Tue, 11 Sept 2018 10:28:54 UTC"))
            .withHeader("Content-Type" , equalTo("application/xml; charset=utf-8"))
            .withHeader("Accept" , equalTo("application/xml"))
            .withHeader("Authorization" , equalTo("Bearer v1-bearer"))
            .withRequestBody(equalTo(expectedStatusPayloadRequest.toString()))
            .willReturn(aResponse()
              .withBody("{}")
              .withStatus(status)
            )
        )
        awaitRequest
        externalWireMockServer.verify(postRequestedFor(anyUrl()))
      }

      "prefix the config key with the prefix if passed" in {
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
}
