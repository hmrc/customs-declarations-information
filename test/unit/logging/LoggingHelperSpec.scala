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

package unit.logging


import org.mockito.Mockito.mock
import play.api.http.HeaderNames._
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.declarations.information.logging.LoggingHelper
import uk.gov.hmrc.customs.declarations.information.model.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.util.CustomHeaderNames
import util.TestData._
import util.UnitSpec

class LoggingHelperSpec extends UnitSpec {

  private def expectedMessage(message: String, maybeAuthorised: String = "") = s"[conversationId=${conversationId.toString}]" +
    "[clientId=some-client-id]" +
    s"[requestedApiVersion=1.0]$maybeAuthorised $message"

  private val requestMock = mock(classOf[Request[Any]])
  private val conversationIdRequest =
    ConversationIdRequest(
      conversationId,
      FakeRequest().withHeaders(
        CONTENT_TYPE -> "A",
        ACCEPT -> "B",
        CustomHeaderNames.XConversationIdHeaderName -> "C",
        CustomHeaderNames.XClientIdHeaderName -> "D",
        "IGNORE" -> "IGNORE"
      )
    )
  private val internalClientIdsRequest = ValidatedHeadersRequest(conversationId, VersionOne, ClientId("some-client-id"), requestMock).toInternalClientIdsRequest(None)

  "LoggingHelper" should {

    "testFormatInfo" in {
      LoggingHelper.formatInfo("Info message", internalClientIdsRequest) shouldBe expectedMessage("Info message")
    }

    "testFormatInfo with authorisation" in {
      LoggingHelper.formatInfo("Info message", internalClientIdsRequest.toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier)))) shouldBe expectedMessage("Info message", "[authorisedAs=Csp(Some(ZZ123456789000), Some(BADGEID123))]")
    }

    "testFormatError" in {
      LoggingHelper.formatError("Error message", internalClientIdsRequest) shouldBe expectedMessage("Error message")
    }

    "testFormatWarn" in {
      LoggingHelper.formatWarn("Warn message", internalClientIdsRequest) shouldBe expectedMessage("Warn message")
    }

    "testFormatDebug" in {
      LoggingHelper.formatDebug("Debug message", internalClientIdsRequest) shouldBe expectedMessage("Debug message")
    }

    "testFormatDebugFull" in {
      LoggingHelper.formatDebugFull("Debug message.", conversationIdRequest) shouldBe s"[conversationId=$conversationIdValue] Debug message. headers=TreeMap(Accept -> B, X-Client-ID -> D, Content-Type -> A, X-Conversation-ID -> C)"
    }
  }

}
