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

import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, UnauthorizedCode, errorBadRequest}
import uk.gov.hmrc.customs.declarations.information.controllers.CustomHeaderNames.XConversationIdHeaderName
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.{AuthAction, HeaderValidator}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.Csp
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ValidatedHeadersRequest
import uk.gov.hmrc.customs.declarations.information.services.CustomsAuthService
import util.UnitSpec
import util.AuthConnectorStubbing
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.TestData._

class AuthActionSpec extends UnitSpec
  with MockitoSugar
  with TableDrivenPropertyChecks
  with BeforeAndAfterEach {

  private lazy val validatedHeadersRequest: ValidatedHeadersRequest[AnyContentAsEmpty.type] = TestValidatedHeadersRequest

  private implicit val ec = Helpers.stubControllerComponents().executionContext
  private val errorResponseUnauthorisedGeneral =
    ErrorResponse(Status.UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")
  private val errorResponseEoriNotFoundInCustomsEnrolment =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "EORI number not found in Customs Enrolment")
  private val errorResponseBadgeIdentifierHeaderMissingOrInvalid =
    errorBadRequest("X-Badge-Identifier header is missing or invalid")
  private val errorResponseMissingIdentifiers =
    errorBadRequest("Both X-Submitter-Identifier and X-Badge-Identifier headers are missing")
  private val errorResponseEoriIdentifierHeaderInvalid =
    errorBadRequest(s"X-Submitter-Identifier header is invalid")
  
  trait SetUp extends AuthConnectorStubbing {
    private val mockLogger= mock[InformationLogger]
    override val mockAuthConnector: AuthConnector = mock[AuthConnector]
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockLogger)
    protected val headerValidator = new HeaderValidator(mockLogger)
    val authAction = new AuthAction(customsAuthService, headerValidator, mockLogger)
  }

  "AuthAction" can {
    "as CSP with eori" should {
      "authorise as CSP when authorised by auth API and both badge identifier and eori are supplied" in new SetUp {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestWithValidBadgeIdEoriPair))
        actual shouldBe Right(TestValidatedHeadersRequestWithValidBadgeIdEoriPair.toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier))))
        verifyNonCspAuthorisationNotCalled
      }

      "authorise as CSP when authorised by auth API and badge identifier is supplied but eori is not" in new SetUp {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestWithValidBadgeIdAndNoEori))
        actual shouldBe Right(TestValidatedHeadersRequestWithValidBadgeIdAndNoEori.toCspAuthorisedRequest(Csp(None, Some(badgeIdentifier))))
        verifyNonCspAuthorisationNotCalled
      }

      "authorise as CSP when authorised by auth API and eori is supplied but badge identifier is not" in new SetUp {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestWithValidEoriAndNoBadgeId))

        actual shouldBe Right(TestValidatedHeadersRequestWithValidEoriAndNoBadgeId.toCspAuthorisedRequest(Csp(Some(declarantEori), None)))
        verifyCspAuthorisationCalled(1)
      }

      "Return 400 response when authorised by auth API and neither badge identifier nor eori is supplied" in new SetUp {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadgeIdNoEori))
        actual shouldBe Left(errorResponseMissingIdentifiers.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API and badge identifier is too long" in new SetUp {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestWithInvalidBadgeIdTooLongAndEori))
        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissingOrInvalid.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API and badge identifier is too short" in new SetUp {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestWithInvalidBadgeIdTooShortAndEori))
        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissingOrInvalid.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API and badge identifier has invalid characters" in new SetUp {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestWithInvalidBadgeIdInvalidCharsAndEori))
        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissingOrInvalid.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API and badge identifier is lowercase" in new SetUp {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestWithInvalidBadgeIdLowercaseAndEori))
        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissingOrInvalid.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API and eori is too long" in new SetUp {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestWithInvalidEoriTooLongAndBadgeId))
        actual shouldBe Left(errorResponseEoriIdentifierHeaderInvalid.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "return InternalError with conversationId when auth call fails" in new SetUp {
        authoriseCspError()

        private val actual = await(authAction.refine(validatedHeadersRequest))
        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(XConversationIdHeaderName -> conversationIdValue))
        verifyCspAuthorisationCalled(1)
      }
    }

    "as non-CSP with eori" should {
      "Authorise as non-CSP when authorised by auth API" in new SetUp {
        authoriseNonCsp(Some(declarantEori))

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadgeIdNoEori))
        actual shouldBe Right(TestValidatedHeadersRequestNoBadgeIdNoEori.toNonCspAuthorisedRequest(declarantEori))
        verifyCspAuthorisationCalled(1)
      }

      "Return 401 response when authorised by auth API but eori does not exist" in new SetUp {
        authoriseNonCsp(maybeEori = None)

        private val actual = await(authAction.refine(validatedHeadersRequest))
        actual shouldBe Left(errorResponseEoriNotFoundInCustomsEnrolment.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
      }

      "Return 401 response when not authorised as non-CSP" in new SetUp {
        unauthoriseCsp()
        unauthoriseNonCspOnly()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadgeIdNoEori))

        actual shouldBe Left(errorResponseUnauthorisedGeneral.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
      }

      "Return 500 response if errors occur in CSP auth API call" in new SetUp {
        unauthoriseCsp()
        authoriseNonCspOnlyError()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadgeIdNoEori))

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
      }
      
    }
  }
}
