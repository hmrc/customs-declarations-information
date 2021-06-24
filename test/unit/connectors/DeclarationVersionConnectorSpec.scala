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

import akka.actor.ActorSystem
import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationVersionConnector
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.customs.declarations.information.services.InformationConfigService
import uk.gov.hmrc.customs.declarations.information.xml.BackendVersionPayloadCreator
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.StatusTestXMLData.expectedStatusPayloadRequest
import util.TestData._
import util.{ApiSubscriptionFieldsTestData, TestData, UnitSpec}

import scala.concurrent.{ExecutionContext, Future}

class DeclarationVersionConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with Eventually {

  private val mockWsPost = mock[HttpClient]
  private val mockLogger = stubInformationLogger
  private val mockServiceConfigProvider = mock[ServiceConfigProvider]
  private val mockInformationConfigService = mock[InformationConfigService]
  private val mockBackendPayloadCreator = mock[BackendVersionPayloadCreator]
  private implicit val ec = Helpers.stubControllerComponents().executionContext

  private val informationCircuitBreakerConfig = InformationCircuitBreakerConfig(50, 1000, 10000)
  private val actorSystem = ActorSystem("mockActorSystem")

  private val connector = new DeclarationVersionConnector(mockWsPost, mockLogger, mockBackendPayloadCreator, mockServiceConfigProvider, mockInformationConfigService, mock[CdsLogger], actorSystem)

  private val v1Config = ServiceConfig("v1-url", Some("v1-bearer"), "v1-default")

  private implicit val asr = AuthorisedRequest(conversationId, VersionOne, ApiSubscriptionFieldsTestData.clientId, None, Csp(Some(declarantEori), Some(badgeIdentifier)), mock[Request[AnyContent]])

  override protected def beforeEach() {
    reset(mockWsPost, mockServiceConfigProvider)
    when(mockServiceConfigProvider.getConfig("declaration-version")).thenReturn(v1Config)
    when(mockInformationConfigService.informationCircuitBreakerConfig).thenReturn(informationCircuitBreakerConfig)
    when(mockBackendPayloadCreator.create(conversationId, correlationId, date, Right(mrn), Some(apiSubscriptionFieldsResponse))(asr)).thenReturn(expectedStatusPayloadRequest)
  }

  private val successfulResponse = HttpResponse(200, "")

  "DeclarationVersionConnector" can {

    "when making a successful request" should {

      "pass URL from config" in {
        returnResponseForRequest(Future.successful(successfulResponse))

        awaitRequest

        verify(mockWsPost).POSTString(ameq(v1Config.url), anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "pass in the body" in {
        returnResponseForRequest(Future.successful(successfulResponse))

        awaitRequest

        verify(mockWsPost).POSTString(anyString, ameq(expectedStatusPayloadRequest.toString()), any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "prefix the config key with the prefix if passed" in {
        returnResponseForRequest(Future.successful(successfulResponse))

        awaitRequest

        verify(mockServiceConfigProvider).getConfig("declaration-version")
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when service call fails with a non-http exception" in {
        returnResponseForRequest(Future.failed(TestData.emulatedServiceFailure))

        val caught = intercept[TestData.EmulatedServiceFailure] {
          awaitRequest
        }
        caught shouldBe TestData.emulatedServiceFailure
      }
    }

    "when configuration is absent" should {
      "throw an exception when no config is found for given api and version combination" in {
        when(mockServiceConfigProvider.getConfig("declaration-version")).thenReturn(null)

        val caught = intercept[IllegalArgumentException] {
          await(connector.send(date, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), Right(mrn)))
        }
        caught.getMessage shouldBe "config not found"
      }
    }
  }

  private def awaitRequest = {
    await(connector.send(date, correlationId, VersionOne, Some(apiSubscriptionFieldsResponse), Right(mrn)))
  }

  private def returnResponseForRequest(eventualResponse: Future[HttpResponse]) = {
    when(mockWsPost.POSTString(anyString, anyString, any[SeqOfHeader])(
      any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext]))
      .thenReturn(eventualResponse)
  }
}
