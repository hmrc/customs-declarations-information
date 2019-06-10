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

package uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders

import javax.inject.{Inject, Singleton}
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, UnauthorizedCode}
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{AuthorisedRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left
import scala.util.control.NonFatal


@Singleton
class AuthAction @Inject()(override val authConnector: AuthConnector,
                           logger: InformationLogger)
                          (implicit ec: ExecutionContext)
  extends ActionRefiner[ValidatedHeadersRequest, AuthorisedRequest] with AuthorisedFunctions  {

  private val errorResponseUnauthorisedGeneral =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")

    override def refine[A](vhr: ValidatedHeadersRequest[A]): Future[Either[Result, AuthorisedRequest[A]]] = {
      implicit val implicitVhr: ValidatedHeadersRequest[A] = vhr
      implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

      authorised(Enrolment("write:customs-declarations-information") and AuthProviders(PrivilegedApplication)) {
        logger.debug("Authorised as CSP")
        Future.successful(Right(vhr.toAuthorisedRequest)) // Simply won't get through if no MRN is specified
      }.recover{
        case NonFatal(_: AuthorisationException) =>
          logger.error("Not authorised")
          Left(errorResponseUnauthorisedGeneral.XmlResult.withConversationId)
        case NonFatal(e) =>
          logger.error("Error authorising CSP", e)
          Left(ErrorInternalServerError.XmlResult.withConversationId)
      }
    }
}
