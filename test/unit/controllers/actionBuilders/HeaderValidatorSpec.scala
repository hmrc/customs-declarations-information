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

package unit.controllers.actionBuilders

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HeaderNames.ACCEPT
import play.api.test.FakeRequest
import play.mvc.Http.Status.BAD_REQUEST
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declarations.information.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.HeaderValidator
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.VersionOne
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{ConversationIdRequest, ExtractedHeaders, ExtractedHeadersImpl}
import uk.gov.hmrc.play.test.UnitSpec
import util.RequestHeaders._
import util.TestData.badgeIdentifier
import util.{ApiSubscriptionFieldsTestData, TestData}

class HeaderValidatorSpec extends UnitSpec with TableDrivenPropertyChecks with MockitoSugar {

  private val extractedHeaders = ExtractedHeadersImpl(VersionOne, badgeIdentifier, ApiSubscriptionFieldsTestData.clientId)
  private val ErrorInvalidBadgeIdentifierHeader: ErrorResponse = ErrorResponse(BAD_REQUEST, BadRequestCode, s"X-Badge-Identifier header is missing or invalid")

  trait SetUp {
    val loggerMock: InformationLogger = mock[InformationLogger]
    val validator = new HeaderValidator(loggerMock)

    def validate(c: ConversationIdRequest[_]): Either[ErrorResponse, ExtractedHeaders] = {
      validator.validateHeaders(c)
    }
  }

  "HeaderValidator" can {
    "in happy path, validation" should {
      "be successful for a valid request with accept header for V1" in new SetUp {
        validate(conversationIdRequest(ValidHeaders)) shouldBe Right(extractedHeaders)
      }
    }
    "in unhappy path, validation" should {
      "fail when request is missing accept header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders - ACCEPT)) shouldBe Left(ErrorAcceptHeaderInvalid)
      }
      "fail when request is missing X-Client-ID header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders - XClientIdHeaderName)) shouldBe Left(ErrorInternalServerError)
      }
      "fail when request has invalid X-Client-ID header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + X_CLIENT_ID_HEADER_INVALID)) shouldBe Left(ErrorInternalServerError)
      }
      "fail when request has invalid accept header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + ACCEPT_HEADER_INVALID)) shouldBe Left(ErrorAcceptHeaderInvalid)
      }
      "fail when request is for V2" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + (ACCEPT -> "application/vnd.hmrc.2.0+xml"))) shouldBe Left(ErrorAcceptHeaderInvalid)
      }
      "fail when request has invalid X-Badge-Identifier header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders + X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_SHORT)) shouldBe Left(ErrorInvalidBadgeIdentifierHeader)
      }
      "fail when request has missing X-Badge-Identifier header" in new SetUp {
        validate(conversationIdRequest(ValidHeaders - X_BADGE_IDENTIFIER_NAME)) shouldBe Left(ErrorInvalidBadgeIdentifierHeader)
      }
    }
  }

  private def conversationIdRequest(requestMap: Map[String, String]): ConversationIdRequest[_] =
    ConversationIdRequest(TestData.conversationId, FakeRequest().withHeaders(requestMap.toSeq: _*))
}
