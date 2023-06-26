/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.SearchParametersCheckAction
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.InternalClientIdsRequest
import uk.gov.hmrc.customs.declarations.information.model.{ClientId, ConversationId, InformationConfig, VersionOne}
import uk.gov.hmrc.customs.declarations.information.services.InformationConfigService
import util.UnitSpec
import util.XmlOps.stringToXml

import java.time.LocalDate
import java.util.UUID

class SearchParametersCheckActionSpec extends UnitSpec  {

  trait SetUp {
    protected implicit val ec = Helpers.stubControllerComponents().executionContext
    private val mockInformationLogger = mock(classOf[InformationLogger])
    private val mockInformationConfigService = mock(classOf[InformationConfigService])

    when(mockInformationConfigService.informationConfig).thenReturn(InformationConfig("url", 30, Seq("ABC123"), false))

    protected val searchParametersCheckAction = new SearchParametersCheckAction(mockInformationLogger, mockInformationConfigService)
    protected val conversationId: ConversationId = ConversationId(UUID.fromString("4e2f2ec0-d82c-46d3-85d6-bac4ef4fc623"))

    protected  def declarationSearchError(code: String, parameterName: String): String =
      s"""<?xml version='1.0' encoding='UTF-8'?>
        |<errorResponse>
        |      <code>${code}</code>
        |      <message>Invalid ${parameterName} parameter</message>
        |</errorResponse>
    """.stripMargin

    protected  def declarationSearchDateError(code: String, parameterName: String): String =
      s"""<?xml version='1.0' encoding='UTF-8'?>
         |<errorResponse>
         |      <code>${code}</code>
         |      <message>Invalid ${parameterName} parameters</message>
         |</errorResponse>
    """.stripMargin

  }

  "SearchParametersCheckAction" should {
    
    "accept all valid parameters parameters" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET",
        "/search?eori=GB123456789000&partyRole=submitter&declarationCategory=IM&goodsLocationCode=BELBELOB4&declarationStatus=all&dateFrom=2021-04-01&dateTo=2021-04-04&pageNumber=2"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).toOption.get
      result.conversationId shouldBe conversationId
    }
    
    "accept a valid partyRole and declarationCategory parameters case insensitive" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=SuBmiTTer&declarationCategory=iM"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).toOption.get
      result.conversationId shouldBe conversationId
    }

    "accept a valid declarationCategory IM" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=consignee&declarationCategory=iM"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).toOption.get
      result.conversationId shouldBe conversationId
    }

    "accept a valid declarationCategory EX" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=SuBmiTTer&declarationCategory=Ex"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).toOption.get
      result.conversationId shouldBe conversationId
    }

    "accept a valid declarationCategory CO" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=SuBmiTTer&declarationCategory=CO"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).toOption.get
      result.conversationId shouldBe conversationId
    }

    "accept a valid declarationCategory ALL" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=SuBmiTTer&declarationCategory=All"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).toOption.get
      result.conversationId shouldBe conversationId
    }


    "accept a valid declarationStatus cleared" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=SuBmiTTer&declarationCategory=All&declarationStatus=cleARed"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).toOption.get
      result.conversationId shouldBe conversationId
    }

    "accept a valid declarationStatus uncleared" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=SuBmiTTer&declarationCategory=All&declarationStatus=UNcleARed"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).toOption.get
      result.conversationId shouldBe conversationId
    }

    "accept a valid declarationStatus rejected" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=SuBmiTTer&declarationCategory=All&declarationStatus=rejecteD"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).toOption.get
      result.conversationId shouldBe conversationId
    }

    "accept a valid declarationStatus all" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=SuBmiTTer&declarationCategory=All&declarationStatus=ALL"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).toOption.get
      result.conversationId shouldBe conversationId
    }

    "reject payload without partyRole" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?declarationCategory=iM"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSearchError("CDS60006", "partyRole"))
    }

    "reject payload with invalid partyRole" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=invalid&declarationCategory=iM"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSearchError("CDS60006", "partyRole"))
    }

    "reject payload with invalid eori" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?eori=invalid-eori-slightly-longer-than-fifty-characters-invalid&partyRole=declarant&declarationCategory=iM"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml("<errorResponse><code>BAD_REQUEST</code><message>Bad Request</message></errorResponse>")
    }

    "reject payload without declarationCategory" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=submitter"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSearchError("CDS60008", "declarationCategory"))
    }

    "reject payload with invalid declarationCategory" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=submitter&declarationCategory=AA"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSearchError("CDS60008", "declarationCategory"))
    }

    "reject payload with invalid declarationStatus" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=submitter&declarationCategory=IM&declarationStatus=invalid"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSearchError("CDS60007", "declarationStatus"))
    }

    "reject payload with dateFrom in the future" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", s"/search?partyRole=submitter&declarationCategory=IM&dateFrom=${LocalDate.now().plusDays(1).toString}"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSearchDateError("CDS60009", "date"))
    }

    "reject payload with dateTo in the future" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", s"/search?partyRole=submitter&declarationCategory=IM&dateTo=${LocalDate.now().plusDays(1).toString}"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSearchDateError("CDS60009", "date"))
    }

    "reject payload with dateTo before dateFrom" in new SetUp {

      private val dateFrom: String = LocalDate.now().toString
      private val dateTo: String = LocalDate.now().minusDays(1).toString

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", s"/search?partyRole=submitter&declarationCategory=IM&dateFrom=$dateFrom&dateTo=$dateTo"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSearchDateError("CDS60009", "date"))
    }

    "reject payload with invalid goodsLocationCode" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=submitter&declarationCategory=IM&goodsLocationCode=AB.,. defghijklmno"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSearchError("CDS60010", "goodsLocationCode"))
    }

    "reject payload with pageNumber set to 0" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=submitter&declarationCategory=IM&pageNumber=0"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSearchError("CDS60012", "pageNumber"))
    }

    "reject payload with pageNumber set to 'A'" in new SetUp {

      private val internalClientIdsRequest = InternalClientIdsRequest(conversationId, VersionOne, ClientId("ABC123"), None, FakeRequest("GET", "/search?partyRole=submitter&declarationCategory=IM&pageNumber=A"))

      private val result = await(searchParametersCheckAction.refine(internalClientIdsRequest)).swap.toOption.get
      status(result) shouldBe BAD_REQUEST
      stringToXml(contentAsString(result)) shouldBe stringToXml(declarationSearchError("CDS60012", "pageNumber"))
    }
  }
}
