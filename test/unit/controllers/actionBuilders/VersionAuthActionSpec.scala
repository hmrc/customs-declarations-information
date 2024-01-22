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

package unit.controllers.actionBuilders

import org.mockito.Mockito.mock
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.test.Helpers
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders.{HeaderValidator, VersionAuthAction}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.Csp
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.services.CustomsAuthService
import util.TestData._
import util.{AuthConnectorStubbing, UnitSpec}

import scala.concurrent.ExecutionContext

class VersionAuthActionSpec extends UnitSpec

  with TableDrivenPropertyChecks
  with BeforeAndAfterEach {

  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext

  trait SetUp extends AuthConnectorStubbing {
    private val mockLogger = mock(classOf[InformationLogger])
    override val mockAuthConnector: AuthConnector = mock(classOf[AuthConnector])
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockLogger)
    protected val headerValidator = new HeaderValidator(mockLogger)
    val authAction = new VersionAuthAction(customsAuthService, headerValidator, mockLogger)
  }

  "AuthAction" can {
    "as CSP with eori" should {
      "authorise as CSP when authorised by auth API and both badge identifier and eori are supplied" in new SetUp {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestWithValidBadgeIdEoriPair.toInternalClientIdsRequest(None)))
        actual shouldBe Right(TestValidatedHeadersRequestWithValidBadgeIdEoriPair.toInternalClientIdsRequest(None).toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier))))
        verifyNonCspAuthorisationNotCalled
      }
    }
  }
}
