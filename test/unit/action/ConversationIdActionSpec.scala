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

package unit.action

import org.mockito.Mockito.mock
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.declarations.information.action.ConversationIdAction
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ConversationIdRequest
import util.TestData.{conversationId, stubUniqueIdsService}
import util.UnitSpec

import scala.concurrent.ExecutionContext

class ConversationIdActionSpec extends UnitSpec {

  trait SetUp {
    protected implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
    private val mockInformationLogger = mock(classOf[InformationLogger])

    val request = FakeRequest()
    val conversationIdAction = new ConversationIdAction(stubUniqueIdsService, mockInformationLogger)
    val expected = ConversationIdRequest(conversationId, request)
  }

  "ConversationIdAction" should {
    "Generate a request containing a unique conversation id" in new SetUp {
      private val actual = await(conversationIdAction.transform(request))

      actual shouldBe expected
    }
  }

}
