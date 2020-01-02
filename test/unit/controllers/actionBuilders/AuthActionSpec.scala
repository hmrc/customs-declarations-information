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

import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, UnauthorizedCode}
import uk.gov.hmrc.customs.declarations.information.controllers.CustomHeaderNames.XConversationIdHeaderName
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.{AuthAction, HeaderValidator}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.Csp
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ValidatedHeadersRequest
import uk.gov.hmrc.customs.declarations.information.services.CustomsAuthService
import uk.gov.hmrc.play.test.UnitSpec
import util.AuthConnectorStubbing
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.TestData._

class AuthActionSpec extends UnitSpec
  with MockitoSugar
  with TableDrivenPropertyChecks
  with BeforeAndAfterEach {

  private lazy val validatedHeadersRequest: ValidatedHeadersRequest[AnyContentAsEmpty.type] = TestValidatedHeadersRequest

  private implicit val ec = Helpers.stubControllerComponents().executionContext
  private val errorResponseEoriNotFoundInCustomsEnrolment =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "EORI number not found in Customs Enrolment")

  trait SetUp extends AuthConnectorStubbing {
    private val mockLogger= mock[InformationLogger]
    override val mockAuthConnector: AuthConnector = mock[AuthConnector]
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockLogger)
    protected val headerValidator = new HeaderValidator(mockLogger)
    val authAction = new AuthAction(customsAuthService, headerValidator, mockLogger)
  }

  //TODO add a lot more tests for all header/CSP/non-CSP scenarios
  "AuthAction" can {
    "for Declaration Status request" should {
      "return AuthorisedRequest for CSP when authorised by auth API" in new SetUp {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithValidBadgeIdEoriPair))

        actual shouldBe Right(validatedHeadersRequestWithValidBadgeIdEoriPair.toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier))))
        verifyCspAuthorisationCalled(1)
      }

      "return InternalError with ConversationId when auth call fails" in new SetUp {
        authoriseCspError()

        private val actual = await(authAction.refine(validatedHeadersRequest))
        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(XConversationIdHeaderName -> conversationIdValue))
        verifyCspAuthorisationCalled(1)
      }

      "Return 401 response when authorised by auth API but Eori does not exist" in new SetUp {
        authoriseNonCsp(maybeEori = None)

        private val actual = await(authAction.refine(validatedHeadersRequest))
        actual shouldBe Left(errorResponseEoriNotFoundInCustomsEnrolment.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
      }
    }
  }
}
