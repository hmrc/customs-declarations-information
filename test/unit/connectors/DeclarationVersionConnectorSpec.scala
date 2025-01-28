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
import com.github.tomakehurst.wiremock.client.WireMock.{verify => _, _}
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{reset, _}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.config.{ConfigService, InformationCircuitBreakerConfig}
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationVersionConnector
import uk.gov.hmrc.customs.declarations.information.model.{AuthorisedRequest, ConnectionError, Csp, VersionOne}
import uk.gov.hmrc.customs.declarations.information.xml.BackendVersionPayloadCreator
import uk.gov.hmrc.http.test.{ExternalWireMockSupport, HttpClientV2Support}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.TestData._
import util.VersionTestXMLData.expectedVersionPayloadRequest
import util.{ApiSubscriptionFieldsTestData, UnitSpec}

import scala.concurrent.ExecutionContext

class DeclarationVersionConnectorSpec
  extends UnitSpec
    with Matchers
    with ScalaFutures
    with HttpClientV2Support
    with IntegrationPatience
    with Eventually
    with BeforeAndAfter
    with ExternalWireMockSupport
{

  private val actorSystem = ActorSystem("mockActorSystem")
  private val mockLogger = stubInformationLogger
  private val mockServiceConfigProvider = mock(classOf[ServiceConfigProvider])
  private val mockInformationConfigService = mock(classOf[ConfigService])
  private val mockBackendPayloadCreator = mock(classOf[BackendVersionPayloadCreator])
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val connector = new DeclarationVersionConnector(httpClientV2, mockLogger, mockBackendPayloadCreator, mockServiceConfigProvider, mockInformationConfigService, mock(classOf[CdsLogger]), actorSystem)

  private implicit val asr: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, VersionOne, ApiSubscriptionFieldsTestData.clientId, None, None, None, Csp(Some(declarantEori), Some(badgeIdentifier)), mock(classOf[Request[AnyContent]]))
  private val informationCircuitBreakerConfig = InformationCircuitBreakerConfig(50, 1000, 10000)


  override protected def beforeEach(): Unit = {
    reset(mockServiceConfigProvider)
    val v1Config = ServiceConfig(externalWireMockUrl, Some("v1-bearer"), "v1-default")
    when(mockServiceConfigProvider.getConfig("declaration-version")).thenReturn(v1Config)
    when(mockInformationConfigService.informationCircuitBreakerConfig).thenReturn(informationCircuitBreakerConfig)
    when(mockBackendPayloadCreator.create(conversationId, correlationId, date, mrn, Some(apiSubscriptionFieldsResponse))(asr)).thenReturn(expectedVersionPayloadRequest)
  }


  "DeclarationVersionConnector" can {

    "when making a successful request" should {

      "pass URL from config" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

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
            .withRequestBody(equalTo("<n1:retrieveDeclarationVersionRequest xsi:schemaLocation=\"http://gov.uk/customs/retrieveDeclarationVersion retrieveDeclarationVersionRequest.xsd\" xmlns:n1=\"http://gov.uk/customs/retrieveDeclarationVersion\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n      <n1:requestCommon>\n        <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>\n        <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>\n        <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>\n        <n1:badgeIdentifier>BADGEID123</n1:badgeIdentifier>\n        <n1:dateTimeStamp>2018-09-11T10:28:54.128Z</n1:dateTimeStamp>\n        <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>\n        <n1:originatingPartyID>ZZ123456789000</n1:originatingPartyID>\n      </n1:requestCommon>\n      <n1:requestDetail>\n        <n1:RetrieveDeclarationVersionRequest>\n          <n1:ServiceRequestParameters>\n            <n1:MRN>theMrn</n1:MRN>\n          </n1:ServiceRequestParameters>\n        </n1:RetrieveDeclarationVersionRequest>\n      </n1:requestDetail>\n    </n1:retrieveDeclarationVersionRequest>"))
            .willReturn(aResponse()
              .withBody("{}")
              .withStatus(200)
            )
        )
        awaitRequest
        externalWireMockServer.verify(postRequestedFor(anyUrl()))
      }
    }
    "pass in the body" in {
      externalWireMockServer.stubFor(
        WireMock
          .post("/")
          .withRequestBody(equalTo(expectedVersionPayloadRequest.toString()))
          .willReturn(aResponse()
            .withBody("{}")
            .withStatus(200)
          )
      )
      awaitRequest
      externalWireMockServer.verify(postRequestedFor(anyUrl()))
    }
  }

  "configuration is absent" should {
    "throw an exception when no config is found for given api and version combination" in {
      when(mockServiceConfigProvider.getConfig(anyString())).thenReturn(null)

      val caught = intercept[IllegalArgumentException] {
        await(connector.send(date, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), mrn))
      }
      caught.getMessage shouldBe "config not found"
    }
  }

  def awaitRequest: Either[ConnectionError, HttpResponse] = {
    await(connector.send(date, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), mrn))
  }
}

