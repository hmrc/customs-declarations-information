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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.config.{ConfigService, InformationCircuitBreakerConfig}
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationFullConnector
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.xml.BackendFullPayloadCreator
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.test.{ExternalWireMockSupport, HttpClientV2Support}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.FullTestXMLData.expectedFullPayloadRequest
import util.TestData._
import util.{ApiSubscriptionFieldsTestData, UnitSpec}

import scala.concurrent.{ExecutionContext, Future}

class DeclarationFullConnectorSpec extends UnitSpec
  with BeforeAndAfterEach
  with Eventually
  with Matchers
  with ScalaFutures
  with HttpClientV2Support
  with IntegrationPatience
  with ExternalWireMockSupport {
  private val actorSystem = ActorSystem("mockActorSystem")
  private val mockRequestBuilder = mock(classOf[RequestBuilder])
  private val mockLogger = stubInformationLogger
  private val mockServiceConfigProvider = mock(classOf[ServiceConfigProvider])
  private val mockInformationConfigService = mock(classOf[ConfigService])
  private val mockBackendPayloadCreator = mock(classOf[BackendFullPayloadCreator])
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val informationCircuitBreakerConfig = InformationCircuitBreakerConfig(50, 1000, 10000)

  private val connector = new DeclarationFullConnector(httpClientV2, mockLogger, mockBackendPayloadCreator, mockServiceConfigProvider, mockInformationConfigService, mock(classOf[CdsLogger]), actorSystem)

  private val v1Config = ServiceConfig(externalWireMockUrl, Some("v1-bearer"), "v1-default")

  private implicit val asr: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, VersionOne, ApiSubscriptionFieldsTestData.clientId, None, None, Some(1), Csp(Some(declarantEori), Some(badgeIdentifier)), mock(classOf[Request[AnyContent]]))

  private val successfulResponse = HttpResponse(200, "")
  //  after {
  //    org.slf4j.MDC.clear()
  //  }

  override protected def beforeEach(): Unit = {
    reset(mockServiceConfigProvider)
    when(mockServiceConfigProvider.getConfig("declaration-full")).thenReturn(v1Config)
    when(mockInformationConfigService.informationCircuitBreakerConfig).thenReturn(informationCircuitBreakerConfig)
    when(mockBackendPayloadCreator.create(conversationId, correlationId, date, mrn, Some(apiSubscriptionFieldsResponse))(asr)).thenReturn(expectedFullPayloadRequest)
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.execute[HttpResponse]).thenReturn(Future.successful(successfulResponse))
  }


  "DeclarationFullConnector" when {

    "making a successful request" should {

      "pass URL from config" in {
        externalWireMockServer.stubFor(
          WireMock
            .post("/")
            .withRequestBody(equalTo("<n1:retrieveFullDeclarationDataRequest xsi:schemaLocation=\"http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd\" xmlns:n1=\"http://gov.uk/customs/FullDeclarationDataRetrievalService\" xmlns:xsi=\"http://gov.uk/customs/retrieveFullDeclarationDataRequest\">\n      <n1:requestCommon>\n        <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>\n        <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>\n        <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>\n        <n1:badgeIdentifier>BADGEID123</n1:badgeIdentifier>\n        <n1:dateTimeStamp>2018-09-11T10:28:54.128Z</n1:dateTimeStamp>\n        <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>\n        <n1:originatingPartyID>ZZ123456789000</n1:originatingPartyID>\n      </n1:requestCommon>\n      <n1:requestDetail>\n            <n1:MRN>theMrn</n1:MRN>\n            <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>\n      </n1:requestDetail>\n    </n1:retrieveFullDeclarationDataRequest>"))
            .willReturn(aResponse()
              .withBody("{}")
              .withStatus(200)
            )
        )
        val res = await(connector.send(date, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), mrn))
        res.fold((err) => {
          println(err)
        }, (result) => {
          println(result)
        })
        externalWireMockServer.verify(postRequestedFor(anyUrl()))
      }

      "pass in the body" in {
        externalWireMockServer.stubFor(
          WireMock
            .post("/")
            .withRequestBody(equalTo(expectedFullPayloadRequest.toString()))
            .willReturn(aResponse()
              .withBody("{}")
              .withStatus(200)
            )
        )
        awaitRequest
        externalWireMockServer.verify(postRequestedFor(anyUrl()))
      }
            "prefix the config key with the prefix if passed" in {
              externalWireMockServer.stubFor(
                WireMock
                  .post("/")
                  .withRequestBody(equalTo("<n1:retrieveFullDeclarationDataRequest xsi:schemaLocation=\"http://gov.uk/customs/FullDeclarationDataRetrievalService retrieveFullDeclarationDataRequest.xsd\" xmlns:n1=\"http://gov.uk/customs/FullDeclarationDataRetrievalService\" xmlns:xsi=\"http://gov.uk/customs/retrieveFullDeclarationDataRequest\">\n      <n1:requestCommon>\n        <n1:clientID>99999999-9999-9999-9999-999999999999</n1:clientID>\n        <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>\n        <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>\n        <n1:badgeIdentifier>BADGEID123</n1:badgeIdentifier>\n        <n1:dateTimeStamp>2018-09-11T10:28:54.128Z</n1:dateTimeStamp>\n        <n1:authenticatedPartyID>ZZ123456789000</n1:authenticatedPartyID>\n        <n1:originatingPartyID>ZZ123456789000</n1:originatingPartyID>\n      </n1:requestCommon>\n      <n1:requestDetail>\n            <n1:MRN>theMrn</n1:MRN>\n            <n1:DeclarationVersionNumber>1</n1:DeclarationVersionNumber>\n      </n1:requestDetail>\n    </n1:retrieveFullDeclarationDataRequest>"))
                  .willReturn(aResponse()
                    .withBody("{}")
                    .withStatus(200)
                  )
              )
              awaitRequest

              verify(mockServiceConfigProvider).getConfig("declaration-full")
            }


      "configuration is absent" should {
        "throw an exception when no config is found for given api and version combination" in {
          when(mockServiceConfigProvider.getConfig("declaration-full")).thenReturn(null)

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
  }
}
