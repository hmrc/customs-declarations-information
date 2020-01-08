/*
 * Copyright 2020 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers._
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.customs.declarations.information.connectors.DeclarationStatusConnector
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.customs.declarations.information.model.{Csp, VersionOne}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.ExternalServicesConfig.{AuthToken, Host, Port}
import util.TestData._
import util._
import util.externalservices.MdgStatusDeclarationService

class DeclarationStatusConnectorSpec extends IntegrationTestSpec
  with GuiceOneAppPerSuite
  with MockitoSugar
  with MdgStatusDeclarationService {

  private lazy val connector = app.injector.instanceOf[DeclarationStatusConnector]

  private val incomingAuthToken = s"Bearer ${ExternalServicesConfig.AuthToken}"
  private val numberOfCallsToTriggerStateChange = 5
  private val unavailablePeriodDurationInMillis = 1000
  private implicit val asr: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, VersionOne,
    ApiSubscriptionFieldsTestData.clientId, Csp(Some(declarantEori), Some(badgeIdentifier)), mock[Request[AnyContent]])
  private implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(incomingAuthToken)))

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    when(mockUuidService.uuid()).thenReturn(correlationIdUuid)
  }

  override protected def afterEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(TestModule.asGuiceableModule)).configure(Map(
      "microservice.services.declaration-status.host" -> Host,
      "microservice.services.declaration-status.port" -> Port,
      "microservice.services.declaration-status.context" -> CustomsDeclarationsExternalServicesConfig.MdgStatusDeclarationServiceContext,
      "microservice.services.declaration-status.bearer-token" -> AuthToken
    )).build()

  "DeclarationStatusConnector" should {

    "make a correct request" in {
      startMdgStatusService()
      await(sendValidXml())
      verifyMdgStatusDecServiceWasCalledWith(requestBody = StatusTestXMLData.expectedDeclarationStatusPayload.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }

    "circuit breaker trips after specified number of failures" in {
      startMdgStatusService(INTERNAL_SERVER_ERROR)

      1 to numberOfCallsToTriggerStateChange foreach { _ =>
        val k = intercept[Upstream5xxResponse](await(sendValidXml()))
        k.reportAs shouldBe BAD_GATEWAY
      }

      1 to 3 foreach { _ =>
        val k = intercept[UnhealthyServiceException](await(sendValidXml()))
        k.getMessage shouldBe "declaration-status"
      }

      resetMockServer()
      startMdgStatusService(ACCEPTED)

      Thread.sleep(unavailablePeriodDurationInMillis)

      1 to 5 foreach { _ =>
        resetMockServer()
        startMdgStatusService(ACCEPTED)
        await(sendValidXml())
        verifyMdgStatusDecServiceWasCalledWith(requestBody = StatusTestXMLData.expectedDeclarationStatusPayload.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
      }
    }

    "return a failed future when external service returns 404" in {
      startMdgStatusService(NOT_FOUND)
      intercept[RuntimeException](await(sendValidXml())).getCause.getClass shouldBe classOf[NotFoundException]
    }

    "return a failed future when external service returns 400" in {
      startMdgStatusService(BAD_REQUEST)
      intercept[RuntimeException](await(sendValidXml())).getCause.getClass shouldBe classOf[BadRequestException]
    }

    "return a failed future when external service returns 500" in {
      startMdgStatusService(INTERNAL_SERVER_ERROR)
      intercept[Upstream5xxResponse](await(sendValidXml()))
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()
      intercept[RuntimeException](await(sendValidXml())).getCause.getClass shouldBe classOf[BadGatewayException]
      startMockServer()
    }
  }

  private def sendValidXml() = {
    connector.send(date, correlationId, VersionOne, apiSubscriptionFieldsResponse, mrn)
  }
}
