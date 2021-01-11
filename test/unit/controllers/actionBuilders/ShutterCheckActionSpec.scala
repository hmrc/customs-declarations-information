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

package unit.controllers.actionbuilders

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.mvc.Result
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ErrorAcceptHeaderInvalid
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.ShutterCheckAction
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.InformationShutterConfig
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ConversationIdRequest
import uk.gov.hmrc.customs.declarations.information.services.InformationConfigService
import util.RequestHeaders.{ACCEPT_HEADER_INVALID, X_CONVERSATION_ID_NAME}
import util.TestData._
import util.UnitSpec

import scala.concurrent.ExecutionContext

class ShutterCheckActionSpec extends UnitSpec with MockitoSugar {

  trait SetUp {
    protected implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
    val mockConfigService = mock[InformationConfigService]
    val mockLogger = mock[InformationLogger]
    val errorResponseVersionShuttered: Result = ErrorResponse(SERVICE_UNAVAILABLE, "SERVER_ERROR", "Service unavailable").XmlResult

    val allVersionsUnshuttered = InformationShutterConfig(Some(false), Some(false))
    val allVersionsShuttered = InformationShutterConfig(Some(true), Some(true))
    val versionOneShuttered = InformationShutterConfig(Some(true), Some(false))
    val versionTwoShuttered = InformationShutterConfig(Some(false), Some(true))
    val allVersionsShutteringUnspecified = InformationShutterConfig(None, None)

  }

  "in happy path, validation" should {
    "be successful for a valid request with accept header for V1" in new SetUp {
      when(mockConfigService.informationShutterConfig).thenReturn(allVersionsUnshuttered)

      val action = new ShutterCheckAction(mockLogger, mockConfigService)

      await(action.refine(TestConversationIdRequestWithV1Headers)) shouldBe Right(TestApiVersionRequestV1)
    }

    "be successful for a valid request with accept header for V2" in new SetUp {
      when(mockConfigService.informationShutterConfig).thenReturn(allVersionsUnshuttered)

      val action = new ShutterCheckAction(mockLogger, mockConfigService)

      await(action.refine(TestConversationIdRequestWithV2Headers)) shouldBe Right(TestApiVersionRequestV2)
    }
  }

  "in unhappy path, validation" should {
    "fail for a valid request with missing accept header" in new SetUp {
      when(mockConfigService.informationShutterConfig).thenReturn(allVersionsUnshuttered)

      val action = new ShutterCheckAction(mockLogger, mockConfigService)
      val result = await(action.refine(ConversationIdRequest(conversationId, FakeRequest())))

      result shouldBe Left(ErrorAcceptHeaderInvalid.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationIdValue))
    }

    "fail for a valid request with invalid accept header" in new SetUp {
      when(mockConfigService.informationShutterConfig).thenReturn(allVersionsUnshuttered)

      val action = new ShutterCheckAction(mockLogger, mockConfigService)
      val requestWithInvalidAcceptHeader = FakeRequest().withHeaders(ACCEPT_HEADER_INVALID)

      val result = await(action.refine(ConversationIdRequest(conversationId, requestWithInvalidAcceptHeader)))
      result shouldBe Left(ErrorAcceptHeaderInvalid.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationIdValue))
    }
  }

  "when shuttered set" should {
    "return 503 error for a valid request with v1 accept header and v1 is shuttered" in new SetUp {
      when(mockConfigService.informationShutterConfig).thenReturn(versionOneShuttered)

      val action = new ShutterCheckAction(mockLogger, mockConfigService)

      val result = await(action.refine(TestConversationIdRequestWithV1Headers))

      result shouldBe Left(errorResponseVersionShuttered)
    }

    "return 503 error for a valid request with v2 accept header and v2 is shuttered" in new SetUp {
      when(mockConfigService.informationShutterConfig).thenReturn(versionTwoShuttered)

      val action = new ShutterCheckAction(mockLogger, mockConfigService)
      val result = await(action.refine(TestConversationIdRequestWithV2Headers))

      result shouldBe Left(errorResponseVersionShuttered)
    }

    "return 503 error for a valid request with v2 accept header and all versions are shuttered" in new SetUp {
      when(mockConfigService.informationShutterConfig).thenReturn(allVersionsShuttered)

      val action = new ShutterCheckAction(mockLogger, mockConfigService)
      val result = await(action.refine(TestConversationIdRequestWithV2Headers))

      result shouldBe Left(errorResponseVersionShuttered)
    }

    "be successful when a valid request with v1 accept header and no shuttering is unspecified" in new SetUp {
      when(mockConfigService.informationShutterConfig).thenReturn(allVersionsShutteringUnspecified)

      val action = new ShutterCheckAction(mockLogger, mockConfigService)
      val result = await(action.refine(TestConversationIdRequestWithV1Headers))

      result shouldBe Right(TestApiVersionRequestV1)
    }
  }
}
