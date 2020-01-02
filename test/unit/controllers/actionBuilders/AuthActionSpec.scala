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

import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, UnauthorizedCode}
import uk.gov.hmrc.customs.declarations.information.controllers.CustomHeaderNames
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.{AuthAction, HeaderValidator}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.Csp
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ValidatedHeadersRequest
import uk.gov.hmrc.customs.declarations.information.services.CustomsAuthService
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData.{TestValidatedHeadersRequest, badgeIdentifier}
import util.{AuthConnectorStubbing, TestData}

class AuthActionSpec extends UnitSpec
  with MockitoSugar
  with TableDrivenPropertyChecks
  with BeforeAndAfterEach {

  private lazy val validatedHeadersRequest: ValidatedHeadersRequest[AnyContentAsEmpty.type] = TestValidatedHeadersRequest

  private val mockAuthenticationConnector: AuthConnector = mock[AuthConnector]
  private val mockLogger= mock[InformationLogger]
  private implicit val ec = Helpers.stubControllerComponents().executionContext

  trait SetUp extends AuthConnectorStubbing {
    override val mockAuthConnector: AuthConnector = mockAuthenticationConnector
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockLogger)
    protected val headerValidator = new HeaderValidator(mockLogger)
  }

  override protected def beforeEach(): Unit = {
    reset(mockAuthenticationConnector)
  }

  "AuthAction" can {
    "for Declaration Status request" should {

//      val authAction = new AuthAction(customsAuthService, mockAuthenticationConnector, mockInformationLogger)

      "return AuthorisedRequest for CSP when authorised by auth API" in new SetUp {
        val authAction = new AuthAction(customsAuthService, headerValidator, mockLogger)
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequest))

        actual shouldBe Right(validatedHeadersRequest.toCspAuthorisedRequest(Csp(None, Some(badgeIdentifier))))
        verifyCspAuthorisationCalled(1)
      }

      "return InternalError with ConversationId when auth call fails" in new SetUp {
        val authAction = new AuthAction(customsAuthService, headerValidator, mockLogger)
        authoriseCspError()

        private val actual = await(authAction.refine(validatedHeadersRequest))
        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(CustomHeaderNames.XConversationIdHeaderName -> TestData.conversationIdValue))
        verifyCspAuthorisationCalled(1)
      }

      "return ErrorResponse with ConversationId when not authorised by auth API" in new SetUp {
        val authAction = new AuthAction(customsAuthService, headerValidator, mockLogger)
        unauthoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequest))
        actual shouldBe Left(ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "Unauthorised request").XmlResult.withHeaders(CustomHeaderNames.XConversationIdHeaderName -> TestData.conversationIdValue))
        verifyCspAuthorisationCalled(1)
      }
    }
  }
}
