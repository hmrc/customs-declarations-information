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

package unit.controllers.actionBuilders

import org.mockito.ArgumentMatchers.any
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HeaderNames.ACCEPT
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declarations.information.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.HeaderValidator
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{ConversationIdRequest, ExtractedHeaders, ExtractedHeadersImpl, HasConversationId}
import uk.gov.hmrc.customs.declarations.information.model.{Eori, VersionOne}
import uk.gov.hmrc.play.test.UnitSpec
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.RequestHeaders._
import util.{ApiSubscriptionFieldsTestData, TestData}

class HeaderValidatorSpec extends UnitSpec with TableDrivenPropertyChecks with MockitoSugar {

  private val extractedHeaders = ExtractedHeadersImpl(VersionOne, ApiSubscriptionFieldsTestData.clientId)

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
      "allow an empty header" in new SetUp {
        validator.eoriMustBeValidIfPresent(conversationIdRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> ""))) shouldBe Right(None)
      }
      "allow only spaces in the header but treat as empty" in new SetUp {
        validator.eoriMustBeValidIfPresent(conversationIdRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> "       "))) shouldBe Right(None)
      }
      "allow headers with leading spaces" in new SetUp {
        validator.eoriMustBeValidIfPresent(conversationIdRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> "  0123456789"))) shouldBe Right(Some(Eori("  0123456789")))
      }
      "allow headers with trailing spaces" in new SetUp {
        validator.eoriMustBeValidIfPresent(conversationIdRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> "0123456789    "))) shouldBe Right(Some(Eori("0123456789    ")))
      }
      "allow headers with embedded spaces" in new SetUp {
        validator.eoriMustBeValidIfPresent(conversationIdRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> "01234  56789"))) shouldBe Right(Some(Eori("01234  56789")))
      }
      "allow special characters" in new SetUp {
        validator.eoriMustBeValidIfPresent(conversationIdRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> "!£$%^&*()-_=+/<>@"))) shouldBe Right(Some(Eori("!£$%^&*()-_=+/<>@")))
      }
      "log info level when valid" in new SetUp {
        validator.eoriMustBeValidIfPresent(conversationIdRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> "ABCABC")))

        PassByNameVerifier(loggerMock, "info")
          .withByNameParam[String]("X-Submitter-Identifier header passed validation: ABCABC")
          .withParamMatcher[HasConversationId](any[HasConversationId])
          .verify()
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
    }
  }

  private def conversationIdRequest(requestMap: Map[String, String]): ConversationIdRequest[_] =
    ConversationIdRequest(TestData.conversationId, FakeRequest().withHeaders(requestMap.toSeq: _*))
}
