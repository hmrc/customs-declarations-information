/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationStatusConnector
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.customs.declarations.information.services.InformationConfigService
import uk.gov.hmrc.customs.declarations.information.xml.MdgPayloadCreator
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.TestData._
import util.{ApiSubscriptionFieldsTestData, StatusTestXMLData, TestData}

import scala.concurrent.{ExecutionContext, Future}

class DeclarationStatusConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with Eventually {

  private val mockWsPost = mock[HttpClient]
  private val mockLogger = stubInformationLogger
  private val mockServiceConfigProvider = mock[ServiceConfigProvider]
  private val mockInformationConfigService = mock[InformationConfigService]
  private val mockMdgPayloadCreator = mock[MdgPayloadCreator]

  private val informationCircuitBreakerConfig = InformationCircuitBreakerConfig(50, 1000, 10000)

  private val connector = new DeclarationStatusConnector(mockWsPost, mockLogger, mockMdgPayloadCreator, mockServiceConfigProvider, mockInformationConfigService)

  private val v1Config = ServiceConfig("v1-url", Some("v1-bearer"), "v1-default")

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val asr = AuthorisedRequest(conversationId, VersionOne, badgeIdentifier, ApiSubscriptionFieldsTestData.clientId, Csp(badgeIdentifier), mock[Request[AnyContent]])

  private val httpException = new NotFoundException("Emulated 404 response from a web call")

  override protected def beforeEach() {
    reset(mockWsPost, mockServiceConfigProvider)
    when(mockServiceConfigProvider.getConfig("declaration-status")).thenReturn(v1Config)
    when(mockInformationConfigService.informationCircuitBreakerConfig).thenReturn(informationCircuitBreakerConfig)
    when(mockMdgPayloadCreator.create(correlationId, date, mrn, dmirId, apiSubscriptionFieldsResponse)(asr)).thenReturn(util.StatusTestXMLData.expectedDeclarationStatusPayload)
  }

  "DeclarationStatusConnector" can {

    "when making a successful request" should {

      "pass URL from config" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        awaitRequest

        verify(mockWsPost).POSTString(ameq(v1Config.url), anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "pass in the body" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        awaitRequest

        verify(mockWsPost).POSTString(anyString, ameq(StatusTestXMLData.expectedDeclarationStatusPayload.toString()), any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "prefix the config key with the prefix if passed" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        awaitRequest

        verify(mockServiceConfigProvider).getConfig("declaration-status")
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when nrs service call fails with a non-http exception" in {
        returnResponseForRequest(Future.failed(TestData.emulatedServiceFailure))

        val caught = intercept[TestData.EmulatedServiceFailure] {
          awaitRequest
        }
        caught shouldBe TestData.emulatedServiceFailure
      }

      "wrap an underlying error when nrs service call fails with an http exception" in {
        returnResponseForRequest(Future.failed(httpException))

        val caught = intercept[RuntimeException] {
          awaitRequest
        }
        caught.getCause shouldBe httpException
      }
    }

    "when configuration is absent" should {
      "throw an exception when no config is found for given api and version combination" in {
        when(mockServiceConfigProvider.getConfig("declaration-status")).thenReturn(null)

        val caught = intercept[IllegalArgumentException] {
          await(connector.send(date, correlationId, dmirId, VersionOne, apiSubscriptionFieldsResponse, mrn))
        }
        caught.getMessage shouldBe "config not found"
      }
    }
  }

  private def awaitRequest = {
    await(connector.send(date, correlationId, dmirId, VersionOne, apiSubscriptionFieldsResponse, mrn))
  }

  private def returnResponseForRequest(eventualResponse: Future[HttpResponse]) = {
    when(mockWsPost.POSTString(anyString, anyString, any[SeqOfHeader])(
      any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext]))
      .thenReturn(eventualResponse)
  }
}
