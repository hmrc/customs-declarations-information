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

package unit.controllers.actionBuilders

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.{contentAsString, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.InternalClientIdsCheckAction
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{ConversationIdRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.customs.declarations.information.model.{ClientId, ConversationId, InformationConfig, VersionOne}
import uk.gov.hmrc.customs.declarations.information.services.InformationConfigService
import util.UnitSpec
import util.XmlOps.stringToXml

import java.util.UUID

class InternalClientIdsCheckActionSpec extends UnitSpec with MockitoSugar {

  trait SetUp {
    protected implicit val ec = Helpers.stubControllerComponents().executionContext
    private val mockInformationLogger = mock[InformationLogger]
    private val mockInformationConfigService = mock[InformationConfigService]

    when(mockInformationConfigService.informationConfig).thenReturn(InformationConfig("url", 30, Seq("ABC123")))

    val request = FakeRequest()
    val internalClientIdAction = new InternalClientIdsCheckAction(mockInformationLogger, mockInformationConfigService)

    val expected = ConversationIdRequest(conversationId, request)

    protected val declarationSubmissionChannelNotValidForStatus: String =
      """<?xml version='1.0' encoding='UTF-8'?>
        |<errorResponse>
        |      <code>BAD_REQUEST</code>
        |      <message>declarationSubmissionChannel parameter not permitted in status request</message>
        |</errorResponse>
    """.stripMargin

    protected val declarationSubmissionChannelInvalid: String =
      """<?xml version='1.0' encoding='UTF-8'?>
        |<errorResponse>
        |      <code>CDS60011</code>
        |      <message>Invalid declarationSubmissionChannel parameter</message>
        |</errorResponse>
    """.stripMargin
    protected val conversationId: ConversationId = ConversationId(UUID.fromString("4e2f2ec0-d82c-46d3-85d6-bac4ef4fc623"))

  }

  "InternalClientIdAction" should {

    "accept version payload without declarationSubmissionChannel from external client id" in new SetUp {

      val validatedHeadersRequest = ValidatedHeadersRequest(conversationId, VersionOne, ClientId("ABC123"),  FakeRequest("GET", "/mrn/ABC/version"))

      val result = await(internalClientIdAction.refine(validatedHeadersRequest)).right.get
      result.conversationId shouldBe conversationId
    }

    "accept valid declarationSubmissionChannel from internal client id" in new SetUp {

      val validatedHeadersRequest = ValidatedHeadersRequest(conversationId, VersionOne, ClientId("ABC123"),  FakeRequest("GET", "/mrn/ABC/version?declarationSubmissionChannel=AuthenticatedPartyOnly"))

      val result = await(internalClientIdAction.refine(validatedHeadersRequest)).right.get
      result.conversationId shouldBe conversationId
    }

    "reject status payload with declarationSubmissionChannel set" in new SetUp {

      val validatedHeadersRequest = ValidatedHeadersRequest(conversationId, VersionOne, ClientId("ABC123"),  FakeRequest("GET", "/status?declarationSubmissionChannel=AuthenticatedPartyOnly"))

      val result = await(internalClientIdAction.refine(validatedHeadersRequest)).left.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSubmissionChannelNotValidForStatus)
    }

    "reject version payload with invalid declarationSubmissionChannel" in new SetUp {

      val validatedHeadersRequest = ValidatedHeadersRequest(conversationId, VersionOne, ClientId("ABC123"),  FakeRequest("GET", "/mrn/ABC/version?declarationSubmissionChannel=INVALID"))

      val result = await(internalClientIdAction.refine(validatedHeadersRequest)).left.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSubmissionChannelInvalid)
    }

    "reject version payload with valid declarationSubmissionChannel and invalid external client id" in new SetUp {

      val validatedHeadersRequest = ValidatedHeadersRequest(conversationId, VersionOne, ClientId("NOTMATCH"),  FakeRequest("GET", "/mrn/ABC/version?declarationSubmissionChannel=AuthenticatedPartyOnly"))

      val result = await(internalClientIdAction.refine(validatedHeadersRequest)).left.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSubmissionChannelInvalid)
    }

  }

}
