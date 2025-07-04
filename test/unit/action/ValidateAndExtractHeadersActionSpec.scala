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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declarations.information.action.ValidateAndExtractHeadersAction
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.{ApiVersionRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.customs.declarations.information.util.HeaderValidator
import util.TestData._
import util.{RequestHeaders, UnitSpec}

import scala.concurrent.ExecutionContext

class ValidateAndExtractHeadersActionSpec extends UnitSpec with TableDrivenPropertyChecks {

  trait SetUp {
    implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
    val mockLogger: InformationLogger = mock(classOf[InformationLogger])
    val mockHeaderStatusValidator: HeaderValidator = mock(classOf[HeaderValidator])
    val validateAndExtractHeadersAction: ValidateAndExtractHeadersAction = new ValidateAndExtractHeadersAction(mockHeaderStatusValidator)
  }

  "HeaderValidationAction when validation succeeds" should {
    "extract headers from incoming request and copy relevant values on to the ValidatedHeaderRequest" in new SetUp {
      val apiVersionRequestV1: ApiVersionRequest[AnyContentAsEmpty.type] = TestApiVersionRequestV1
      when(mockHeaderStatusValidator.extractClientIdHeaderIfPresentAndValid(any[ApiVersionRequest[Any]])).thenReturn(Right(TestExtractedHeaders))

      val actualResult: Either[Result, ValidatedHeadersRequest[_]] = await(validateAndExtractHeadersAction.refine(apiVersionRequestV1))

      actualResult shouldBe Right(TestValidatedHeadersRequest)
    }
  }

  "HeaderValidationAction when validation fails" should {
    "return error with conversation Id in the headers" in new SetUp {
      val apiVersionRequestV1: ApiVersionRequest[AnyContentAsEmpty.type] = TestApiVersionRequestV1
      when(mockHeaderStatusValidator.extractClientIdHeaderIfPresentAndValid(any[ApiVersionRequest[Any]])).thenReturn(Left(ErrorContentTypeHeaderInvalid))

      val actualResult: Either[Result, ValidatedHeadersRequest[_]] = await(validateAndExtractHeadersAction.refine(apiVersionRequestV1))

      actualResult shouldBe Left(ErrorContentTypeHeaderInvalid.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationIdValue))
    }
  }
}
