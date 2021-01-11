/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers
import uk.gov.hmrc.customs.declarations.information.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.{ApiSubscriptionFieldsResponse, InformationConfig}
import uk.gov.hmrc.customs.declarations.information.services.InformationConfigService
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, HttpReads}
import util.CustomsDeclarationsExternalServicesConfig.ApiSubscriptionFieldsContext
import util.ExternalServicesConfig._
import util.{ApiSubscriptionFieldsTestData, TestData, UnitSpec}

import scala.concurrent.{ExecutionContext, Future}

class ApiSubscriptionFieldsConnectorSpec extends UnitSpec
  with MockitoSugar
  with BeforeAndAfterEach
  with Eventually
  with ApiSubscriptionFieldsTestData {

  private val mockWSGetImpl = mock[HttpClient]
  private val mockLogger = mock[InformationLogger]
  private val mockInformationConfigService = mock[InformationConfigService]
  private val mockInformationConfig = mock[InformationConfig]
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val vpr = TestData.TestCspAuthorisedRequest
  private implicit val ec = Helpers.stubControllerComponents().executionContext

  private val connector = new ApiSubscriptionFieldsConnector(mockWSGetImpl, mockLogger, mockInformationConfigService)

  private val badRequestException = new BadRequestException("Emulated 400 response from a web call")
  private val expectedUrl = s"http://$Host:$Port$ApiSubscriptionFieldsContext/application/SOME_X_CLIENT_ID/context/some/api/context/version/1.0"

  override protected def beforeEach() {
    reset(mockLogger, mockWSGetImpl, mockInformationConfigService)

    when(mockInformationConfigService.informationConfig).thenReturn(mockInformationConfig)
    when(mockInformationConfig.apiSubscriptionFieldsBaseUrl).thenReturn(s"http://$Host:$Port$ApiSubscriptionFieldsContext")
  }

  "ApiSubscriptionFieldsConnector" can {
    "when making a successful request" should {
      "use the correct URL for valid path parameters and config" in {
        val futureResponse = Future.successful(apiSubscriptionFieldsResponse)
        when(mockWSGetImpl.GET[ApiSubscriptionFieldsResponse](
          ameq(expectedUrl))
          (any[HttpReads[ApiSubscriptionFieldsResponse]](), any[HeaderCarrier](), any[ExecutionContext])).thenReturn(futureResponse)

        awaitRequest shouldBe apiSubscriptionFieldsResponse
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when api subscription fields call fails with a non-http exception" in {
        returnResponseForRequest(Future.failed(TestData.emulatedServiceFailure))

        val caught = intercept[TestData.EmulatedServiceFailure] {
          awaitRequest
        }

        caught shouldBe TestData.emulatedServiceFailure
      }

      "wrap an underlying error when api subscription fields call fails with a bad request exception" in {
        returnResponseForRequest(Future.failed(badRequestException))

        val caught = intercept[RuntimeException] {
          awaitRequest
        }

        caught.getCause shouldBe badRequestException
      }
    }
  }

  private def awaitRequest = {
    await(connector.getSubscriptionFields(apiSubscriptionKey))
  }

  private def returnResponseForRequest(eventualResponse: Future[ApiSubscriptionFieldsResponse]) = {
    when(mockWSGetImpl.GET[ApiSubscriptionFieldsResponse](anyString())
      (any[HttpReads[ApiSubscriptionFieldsResponse]](), any[HeaderCarrier](), any[ExecutionContext])).thenReturn(eventualResponse)
  }

}
