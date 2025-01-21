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
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.config.{ConfigService, InformationCircuitBreakerConfig}
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationVersionConnector
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.xml.BackendVersionPayloadCreator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.test.HttpClientV2Support
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.TestData._
import util.VersionTestXMLData.expectedVersionPayloadRequest
import util.{ApiSubscriptionFieldsTestData, UnitSpec}
import play.api.libs.json.Json

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class DeclarationVersionConnectorSpec extends UnitSpec with BeforeAndAfterEach with Eventually with HttpClientV2Support with ScalaFutures {
//  trait HttpClient extends HttpClientV2 with HttpClientV2Support
  private val mockWsPost = mock(classOf[HttpClientV2],RETURNS_MOCKS)
  private val mockRequestBuilder = mock(classOf[RequestBuilder], RETURNS_MOCKS)
  private val mockLogger = stubInformationLogger
  private val mockServiceConfigProvider = mock(classOf[ServiceConfigProvider])
  private val mockInformationConfigService = mock(classOf[ConfigService])
  private val mockBackendPayloadCreator = mock(classOf[BackendVersionPayloadCreator])
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val informationCircuitBreakerConfig = InformationCircuitBreakerConfig(50, 1000, 10000)
  private val actorSystem = ActorSystem("mockActorSystem")

  private val connector = new DeclarationVersionConnector(mockWsPost, mockLogger, mockBackendPayloadCreator, mockServiceConfigProvider, mockInformationConfigService, mock(classOf[CdsLogger]), actorSystem)

  private val v1Config = ServiceConfig("https://localhost:3000/", Some("v1-bearer"), "v1-default")
  private val url: URL = url"${v1Config.url}"

  private val successfulResponse = HttpResponse(200, "")


  private implicit val asr: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, VersionOne, ApiSubscriptionFieldsTestData.clientId, None, None, None, Csp(Some(declarantEori), Some(badgeIdentifier)), mock(classOf[Request[AnyContent]]))

  override protected def beforeEach(): Unit = {
    reset(mockWsPost, mockServiceConfigProvider)
    when(mockServiceConfigProvider.getConfig("declaration-version")).thenReturn(v1Config)
    when(mockInformationConfigService.informationCircuitBreakerConfig).thenReturn(informationCircuitBreakerConfig)
    when(mockBackendPayloadCreator.create(conversationId, correlationId, date, mrn, Some(apiSubscriptionFieldsResponse))(asr)).thenReturn(expectedVersionPayloadRequest)
    when(mockWsPost.post(any())(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.execute[HttpResponse]).thenReturn(Future.successful(successfulResponse))

  }


  "DeclarationVersionConnector" can {

    "when making a successful request" should {

      "pass URL from config" in {
//        returnResponseForRequest(successfulResponse)

        awaitRequest

      verify(mockWsPost).post(url)(hc).withBody(Json.obj()).execute[HttpResponse]



//        awaitRequest
//        verify(mockWsPost).post(ameq(v1Config.url).asInstanceOf[URL]).withBody((Json.obj())).execute[HttpResponse]
        //thenReturn  (eventualResponse)
        //verify(mockWsPost).POSTString(ameq(v1Config.url), anyString, any[Seq[(String, String)]])(
          //any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "pass in the body" in {
//        returnResponseForRequest(Future.successful(successfulResponse))

        awaitRequest
        val url:URL = ameq(v1Config.url).asInstanceOf[URL]
        verify(mockWsPost).post(url).withBody(ameq(expectedVersionPayloadRequest.toString())).execute
        //verify(mockWsPost).POSTString(anyString, ameq(expectedVersionPayloadRequest.toString()), any[Seq[(String, String)]])(
          //any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "prefix the config key with the prefix if passed" in {
//        returnResponseForRequest(Future.successful(successfulResponse))

        awaitRequest

        verify(mockServiceConfigProvider).getConfig("declaration-version")
      }
    }

    "when configuration is absent" should {
      "throw an exception when no config is found for given api and version combination" in {
        when(mockServiceConfigProvider.getConfig("declaration-version")).thenReturn(null)

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

//  private def returnResponseForRequest(response: HttpResponse) = {
//    //val url:URL = ameq(v1Config.url).asInstanceOf[URL]
//    //when(mockWsPost.post(ameq(v1Config.url).asInstanceOf[URL]).setHeader(any()).withBody(anyString()).execute) thenReturn (eventualResponse)
////    when(mockWsPost.post(url"http://localhost:3000/").withBody(Json.obj()).execute) thenReturn(eventualResponse)
////    httpClientV2.post(url"${v1Config.url}").withBody(Json.obj()).execute[HttpResponse]
//
//    when(mockWsPost
//      .post(ameq(url))(ameq(hc))
//      .withBody(Json.obj())
//      .execute[HttpResponse]
//
//    ) thenReturn Future.successful(response)
//  }
}
