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

package unit.controllers.actionBuilders

import org.mockito.Mockito.{mock, when}
import play.api.test.Helpers.{contentAsString, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.DeclarationFullCheckAction
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{ConversationIdRequest, InternalClientIdsRequest}
import uk.gov.hmrc.customs.declarations.information.model.{ClientId, ConversationId, InformationConfig, VersionOne}
import uk.gov.hmrc.customs.declarations.information.services.InformationConfigService
import util.TestData.declarationSubmissionChannel
import util.UnitSpec
import util.XmlOps.stringToXml

import java.util.UUID
import scala.concurrent.ExecutionContext

class DeclarationFullCheckActionSpec extends UnitSpec {

  trait SetUp {
    protected implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
    private val mockInformationLogger = mock(classOf[InformationLogger])
    private val mockInformationConfigService = mock(classOf[InformationConfigService])

    when(mockInformationConfigService.informationConfig).thenReturn(InformationConfig("url", 30, Seq("ABC123")))

    val request = FakeRequest()
    val fullDeclarationCheckAction = new DeclarationFullCheckAction(mockInformationLogger, mockInformationConfigService)

    val expected = ConversationIdRequest(conversationId, request)

    protected val declarationSubmissionChannelNotValidForStatus: String =
      """<?xml version='1.0' encoding='UTF-8'?>
        |<errorResponse>
        |      <code>BAD_REQUEST</code>
        |      <message>declarationSubmissionChannel parameter not permitted in status request</message>
        |</errorResponse>
    """.stripMargin

    protected val declarationFullInvalid: String =
      """<?xml version='1.0' encoding='UTF-8'?>
        |<errorResponse>
        |      <code>BAD_REQUEST</code>
        |      <message>Invalid declarationVersion parameter</message>
        |</errorResponse>
    """.stripMargin
    protected val conversationId: ConversationId = ConversationId(UUID.fromString("4e2f2ec0-d82c-46d3-85d6-bac4ef4fc623"))

  }

  "InternalClientIdAction" should {

    "accept version payload without declarationSubmissionChannel from external client id" in new SetUp {

      val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/mrn/ABC/version"))

      val result = await(fullDeclarationCheckAction.refine(internalClientIdsRequest)).toOption.get
      result.conversationId shouldBe conversationId
    }

    "accept valid declarationSubmissionChannel from internal client id" in new SetUp {

      val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), declarationSubmissionChannel, FakeRequest("GET", "/mrn/ABC/version?declarationSubmissionChannel=AuthenticatedPartyOnly"))

      val result = await(fullDeclarationCheckAction.refine(internalClientIdsRequest)).toOption.get
      result.conversationId shouldBe conversationId
    }

    "accept declarationVersion set to 1" in new SetUp {

      val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), declarationSubmissionChannel, FakeRequest("GET", "/mrn/ABC/version?declarationSubmissionChannel=AuthenticatedPartyOnly&declarationVersion=1"))

      val result = await(fullDeclarationCheckAction.refine(internalClientIdsRequest)).toOption.get
      result.conversationId shouldBe conversationId
    }

    "reject declarationVersion set to -1" in new SetUp {

      val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), declarationSubmissionChannel, FakeRequest("GET", "/mrn/ABC/version?declarationSubmissionChannel=AuthenticatedPartyOnly&declarationVersion=-1"))

      val result = await(fullDeclarationCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationFullInvalid)
    }

    "reject declarationVersion set to AA" in new SetUp {

      val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), declarationSubmissionChannel, FakeRequest("GET", "/mrn/ABC/version?declarationSubmissionChannel=AuthenticatedPartyOnly&declarationVersion=AA"))

      val result = await(fullDeclarationCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationFullInvalid)
    }
  }
}
