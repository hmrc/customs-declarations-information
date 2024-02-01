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

package integration

import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declarations.information.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.http._
import util.ExternalServicesConfig.{Host, Port}
import util._
import util.externalservices.ApiSubscriptionFieldsService

class ApiSubscriptionFieldsConnectorSpec extends IntegrationTestSpec
  with GuiceOneAppPerSuite
  with ApiSubscriptionFieldsService
  with ApiSubscriptionFieldsTestData {

  private lazy val connector = app.injector.instanceOf[ApiSubscriptionFieldsConnector]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private implicit val vpr: AuthorisedRequest[AnyContentAsEmpty.type] = TestData.TestCspAuthorisedRequest
  private implicit val mockInformationLogger: InformationLogger = mock(classOf[InformationLogger])

  override protected def beforeAll(): Unit = {
    startMockServer()
  }

  override protected def beforeEach(): Unit = {
    reset(mockInformationLogger)
    resetMockServer()
  }

  override protected def afterAll(): Unit = {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(IntegrationTestModule(mockInformationLogger))).configure(Map(
      "microservice.services.api-subscription-fields.host" -> Host,
      "microservice.services.api-subscription-fields.port" -> Port,
      "microservice.services.api-subscription-fields.context" -> CustomsDeclarationsExternalServicesConfig.ApiSubscriptionFieldsContext,
      "metrics.enabled" -> false
    )).build()

  "ApiSubscriptionFieldsConnector" should {

    "make a correct request" in {
      setupGetSubscriptionFieldsToReturn()

      val response = await(connector.getSubscriptionFields(apiSubscriptionKey))

      response shouldBe Some(apiSubscriptionFieldsResponse)
      verifyGetSubscriptionFieldsCalled()
    }

    "return a failed future when external service returns 404" in {
      setupGetSubscriptionFieldsToReturn(NOT_FOUND)

      val response = await(connector.getSubscriptionFields(apiSubscriptionKey))

      response shouldBe None
    }

    "return a failed future when external service returns 400" in {
      setupGetSubscriptionFieldsToReturn(BAD_REQUEST)

      val response = await(connector.getSubscriptionFields(apiSubscriptionKey))

      response shouldBe None
    }

    "return a failed future when external service returns any 4xx response other than 400" in {
      setupGetSubscriptionFieldsToReturn(FORBIDDEN)

      val response = await(connector.getSubscriptionFields(apiSubscriptionKey))

      response shouldBe None
    }

    "return a failed future when external service returns 500" in {
      setupGetSubscriptionFieldsToReturn(INTERNAL_SERVER_ERROR)

      val response = await(connector.getSubscriptionFields(apiSubscriptionKey))

      response shouldBe None
    }
  }
}
