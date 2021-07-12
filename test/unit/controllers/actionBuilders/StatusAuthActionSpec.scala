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
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.{AuthAction, HeaderValidator, StatusAuthAction}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.Csp
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ValidatedHeadersRequest
import uk.gov.hmrc.customs.declarations.information.services.CustomsAuthService
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.TestData._
import util.{AuthConnectorStubbing, UnitSpec}

class StatusAuthActionSpec extends UnitSpec
  with MockitoSugar
  with TableDrivenPropertyChecks
  with BeforeAndAfterEach {

  private implicit val ec = Helpers.stubControllerComponents().executionContext

  trait SetUp extends AuthConnectorStubbing {
    private val mockLogger= mock[InformationLogger]
    override val mockAuthConnector: AuthConnector = mock[AuthConnector]
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockLogger)
    protected val headerValidator = new HeaderValidator(mockLogger)
    val authAction = new StatusAuthAction(customsAuthService, headerValidator, mockLogger)
  }

  "AuthAction" can {
    "as CSP with eori" should {
      "authorise as CSP when authorised by auth API and both badge identifier and eori are supplied" in new SetUp {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestWithValidBadgeIdEoriPair))
        actual shouldBe Right(TestValidatedHeadersRequestWithValidBadgeIdEoriPair.toInternalClientIdsRequest(None).toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier))))
        verifyNonCspAuthorisationNotCalled
      }
    }
  }
}
