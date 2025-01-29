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

import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.config.{ConfigService, InformationConfig}
import uk.gov.hmrc.customs.declarations.information.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.AuthorisedRequest
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util.CustomsDeclarationsExternalServicesConfig.ApiSubscriptionFieldsContext
import util.ExternalServicesConfig._
import util.{ApiSubscriptionFieldsTestData, TestData, UnitSpec}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class ApiSubscriptionFieldsConnectorSpec extends UnitSpec
  with BeforeAndAfterEach
  with Eventually
  with ApiSubscriptionFieldsTestData
  with ScalaFutures
  with HttpClientV2Support {

  private val mockWSGetImpl = mock(classOf[HttpClientV2])
  private val mockLogger = {
    val mockServicesConfig = mock(classOf[ServicesConfig])
    when(mockServicesConfig.getString(any[String])).thenReturn("customs-declarations-information")
    new InformationLogger(new CdsLogger(mockServicesConfig))
  }
  private val mockInformationConfigService = mock(classOf[ConfigService])
  private val mockInformationConfig = mock(classOf[InformationConfig])
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val vpr: AuthorisedRequest[AnyContentAsEmpty.type] = TestData.TestCspAuthorisedRequest
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private val mockRequestBuilder = mock(classOf[RequestBuilder])

  private val connector = new ApiSubscriptionFieldsConnector(mockWSGetImpl, mockLogger, mockInformationConfigService)

  private val expectedUrl = new URL(s"http://$Host:$Port$ApiSubscriptionFieldsContext/application/SOME_X_CLIENT_ID/context/some/api/context/version/1.0")

  override protected def beforeEach(): Unit = {
    reset(mockWSGetImpl, mockInformationConfigService)
    when(mockInformationConfigService.informationConfig).thenReturn(mockInformationConfig)
    when(mockInformationConfig.apiSubscriptionFieldsBaseUrl).thenReturn(s"http://$Host:$Port$ApiSubscriptionFieldsContext")
  }

  "ApiSubscriptionFieldsConnector" can {
    "when making a successful request" should {
      "use the correct URL for valid path parameters and config" in {
        val futureResponse = Future.successful(HttpResponse(OK, responseJsonString))
        when(mockWSGetImpl.get(ameq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(futureResponse))
        awaitRequest() shouldBe Some(apiSubscriptionFieldsResponse)
      }
    }

    "when making an failing request" should {

      "wrap an underlying error when api subscription fields call fails with a bad request exception" in {
        returnResponseForRequest(Future.successful(Future.successful(HttpResponse(BAD_REQUEST, ""))))

        awaitRequest() shouldBe None
      }
    }
  }

  private def awaitRequest() = {
    await(connector.getSubscriptionFields(apiSubscriptionKey))
  }

  private def returnResponseForRequest(eventualResponse: Future[HttpResponse]) = {
    when(mockWSGetImpl.get(any[URL]())(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(eventualResponse)
  }

}
