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

import org.mockito.ArgumentMatchers.any
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declarations.information.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.HeaderValidator
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{ApiVersionRequest, ExtractedHeaders, ExtractedHeadersImpl, HasConversationId}
import uk.gov.hmrc.customs.declarations.information.model.{ApiVersion, Eori, VersionOne, VersionTwo}
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.RequestHeaders._
import util.{ApiSubscriptionFieldsTestData, TestData, UnitSpec}

class HeaderValidatorSpec extends UnitSpec with TableDrivenPropertyChecks with MockitoSugar {

  private val extractedHeaders = ExtractedHeadersImpl(ApiSubscriptionFieldsTestData.clientId)

  trait SetUp {
    val loggerMock: InformationLogger = mock[InformationLogger]
    val validator = new HeaderValidator(loggerMock)

    def validate(avr: ApiVersionRequest[_]): Either[ErrorResponse, ExtractedHeaders] = {
      validator.validateHeaders(avr)
    }
  }

  "HeaderValidator" can {
    "in happy path, validation" should {
      "be successful for a valid request with accept header for V1" in new SetUp {
        validate(apiVersionRequest(ValidHeaders)) shouldBe Right(extractedHeaders)
      }
      "be successful for a valid request with accept header for V2" in new SetUp {
        validate(apiVersionRequest(ValidHeaders + ACCEPT_HMRC_XML_HEADER_V2, VersionTwo)) shouldBe Right(extractedHeaders)
      }
      "allow an empty header" in new SetUp {
        validator.eoriMustBeValidIfPresent(apiVersionRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> ""))) shouldBe Right(None)
      }
      "allow only spaces in the header but treat as empty" in new SetUp {
        validator.eoriMustBeValidIfPresent(apiVersionRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> "       "))) shouldBe Right(None)
      }
      "allow headers with leading spaces" in new SetUp {
        validator.eoriMustBeValidIfPresent(apiVersionRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> "  0123456789"))) shouldBe Right(Some(Eori("  0123456789")))
      }
      "allow headers with trailing spaces" in new SetUp {
        validator.eoriMustBeValidIfPresent(apiVersionRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> "0123456789    "))) shouldBe Right(Some(Eori("0123456789    ")))
      }
      "allow headers with embedded spaces" in new SetUp {
        validator.eoriMustBeValidIfPresent(apiVersionRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> "01234  56789"))) shouldBe Right(Some(Eori("01234  56789")))
      }
      "allow special characters" in new SetUp {
        validator.eoriMustBeValidIfPresent(apiVersionRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> "!£$%^&*()-_=+/<>@"))) shouldBe Right(Some(Eori("!£$%^&*()-_=+/<>@")))
      }
      "log info level when valid" in new SetUp {
        validator.eoriMustBeValidIfPresent(apiVersionRequest(ValidHeaders + (X_SUBMITTER_IDENTIFIER_NAME -> "ABCABC")))

        PassByNameVerifier(loggerMock, "info")
          .withByNameParam[String]("X-Submitter-Identifier header passed validation: ABCABC")
          .withParamMatcher[HasConversationId](any[HasConversationId])
          .verify()
      }
      
    }
    "in unhappy path, validation" should {
      "fail when request is missing X-Client-ID header" in new SetUp {
        validate(apiVersionRequest(ValidHeaders - XClientIdHeaderName)) shouldBe Left(ErrorInternalServerError)
      }
      "fail when request has invalid X-Client-ID header" in new SetUp {
        validate(apiVersionRequest(ValidHeaders + X_CLIENT_ID_HEADER_INVALID)) shouldBe Left(ErrorInternalServerError)
      }
    }
  }

  private def apiVersionRequest(requestMap: Map[String, String], apiVersion: ApiVersion = VersionOne): ApiVersionRequest[_] =
    ApiVersionRequest(TestData.conversationId, apiVersion, FakeRequest().withHeaders(requestMap.toSeq: _*))
}
