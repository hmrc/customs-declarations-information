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

package uk.gov.hmrc.customs.declarations.information.services

import javax.inject.{Inject, Singleton}
import play.api.http.Status
import play.api.http.Status.UNAUTHORIZED
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, PrivilegedApplication}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, UnauthorizedCode}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declarations.information.model.{Eori, NonCsp}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left
import scala.util.control.NonFatal

@Singleton
class CustomsAuthService @Inject()(override val authConnector: AuthConnector,
                                   logger: InformationLogger)
                                  (implicit ec: ExecutionContext) extends AuthorisedFunctions {

  private val hmrcCustomsEnrolment = "HMRC-CUS-ORG"
  private val customsDeclarationEnrolment = "write:customs-declarations-information"

  private lazy val errorResponseEoriNotFoundInCustomsEnrolment =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "EORI number not found in Customs Enrolment")
  private val errorResponseUnauthorisedGeneral =
    ErrorResponse(Status.UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")

  type IsCsp = Boolean

  /*
  Wrapper around HMRC authentication library authorised function for CSP authentication
  */
  def authAsCsp(implicit vhr: HasConversationId, hc: HeaderCarrier): Future[Either[ErrorResponse, IsCsp]] = {
    val eventualAuth: Future[Either[ErrorResponse, IsCsp]] =
        authorised(Enrolment(customsDeclarationEnrolment) and AuthProviders(PrivilegedApplication)) {
          Future.successful[Either[ErrorResponse, IsCsp]] {
            logger.debug(s"Successfully authorised CSP PrivilegedApplication with $customsDeclarationEnrolment enrolment")
            Right(true)
          }
        }

    eventualAuth.recover{
      case NonFatal(ae: AuthorisationException) =>
        logger.debug(s"No authorisation for CSP PrivilegedApplication with $customsDeclarationEnrolment enrolment", ae)
        Right(false)
      case NonFatal(e) =>
        logger.error(s"Error when authorising for CSP PrivilegedApplication with $customsDeclarationEnrolment enrolment", e)
        Left(ErrorInternalServerError)
    }
  }

  /*
    Wrapper around HMRC authentication library authorised function for non-CSP authentication
    */
  def authAsNonCsp(implicit vhr: HasConversationId, hc: HeaderCarrier): Future[Either[ErrorResponse, NonCsp]] = {
    val eventualAuth: Future[Enrolments] =
        authorised(Enrolment(hmrcCustomsEnrolment) and AuthProviders(GovernmentGateway)).retrieve(Retrievals.authorisedEnrolments) {
          logger.debug(s"Successfully authorised non-CSP with $hmrcCustomsEnrolment enrolment and GovernmentGateway non-CSP authorisedEnrolment retrievals pending eori check")
          enrolments =>
            Future.successful(enrolments)
        }

    eventualAuth.map{ enrolments =>
      val maybeEori: Option[Eori] = findEoriInCustomsEnrolment(enrolments, hc.authorization)
      logger.debug(s"eori from $hmrcCustomsEnrolment enrolment for non-CSP request: $maybeEori")
      maybeEori.fold[Either[ErrorResponse, NonCsp]]{
        logger.debug(s"No authorisation for non-CSP with $hmrcCustomsEnrolment enrolment due to no eori in $hmrcCustomsEnrolment enrolment")
        Left(errorResponseEoriNotFoundInCustomsEnrolment)
      }{ eori =>
        logger.debug(s"Successfully authorised non-CSP with $hmrcCustomsEnrolment and using eori: ${eori.toString}")
        Right(NonCsp(eori))
      }
    }.recover{
      case NonFatal(ae: AuthorisationException) =>
        logger.debug(s"No authorisation for non-CSP with $hmrcCustomsEnrolment enrolment", ae)
        Left(errorResponseUnauthorisedGeneral)
      case NonFatal(e) =>
        logger.error(s"Error when authorising for non-CSP with $hmrcCustomsEnrolment enrolment", e)
        Left(ErrorInternalServerError)
    }
  }

  private def findEoriInCustomsEnrolment[A](enrolments: Enrolments, authHeader: Option[Authorization])(implicit vhr: HasConversationId, hc: HeaderCarrier): Option[Eori] = {
    val maybeCustomsEnrolment = enrolments.getEnrolment(hmrcCustomsEnrolment)
    if (maybeCustomsEnrolment.isEmpty) {
      logger.warn(s"$hmrcCustomsEnrolment enrolment not retrieved for non-CSP request")
    }
    for {
      customsEnrolment <- maybeCustomsEnrolment
      eoriIdentifier <- customsEnrolment.getIdentifier("EORINumber")
    } yield Eori(eoriIdentifier.value)
  }

}
