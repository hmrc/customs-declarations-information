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

package uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.declarations.information.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{AuthorisedRequest, HasConversationId, HasRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.customs.declarations.information.model.{AuthorisedAsCsp, BadgeIdentifier, Csp, Eori}
import uk.gov.hmrc.customs.declarations.information.services.CustomsAuthService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left

/** Action builder that attempts to authorise request as a CSP or else NON CSP
 * <ul>
 * <li/>INPUT - `ValidatedHeadersRequest`
 * <li/>OUTPUT - `AuthorisedRequest` - authorised will be `AuthorisedAs.Csp` or `AuthorisedAs.NonCsp`
 * <li/>ERROR -
 * <ul>
 * <li/>400 if authorised as CSP but both badge identifier and submitter identifier not present for CSP
 * <li/>401 if authorised as non-CSP but enrolments does not contain an EORI.
 * <li/>401 if not authorised as CSP or non-CSP
 * <li/>500 on any downstream errors returning 500
 * </ul>
 * </ul>
 */
@Singleton
class AuthAction @Inject()(customsAuthService: CustomsAuthService,
                           headerValidator: HeaderValidator,
                           logger: InformationLogger)
                          (implicit ec: ExecutionContext)
  extends ActionRefiner[ValidatedHeadersRequest, AuthorisedRequest] {

  protected[this] def executionContext: ExecutionContext = ec

  private def errorResponseMissingIdentifiers = errorBadRequest(s"Both $XSubmitterIdentifierHeaderName and $XBadgeIdentifierHeaderName headers are missing")

  override def refine[A](vhr: ValidatedHeadersRequest[A]): Future[Either[Result, AuthorisedRequest[A]]] = {
    implicit val implicitVhr: ValidatedHeadersRequest[A] = vhr
    implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

    authAsCspWithMandatoryAuthHeaders.flatMap{
      case Right(maybeAuthorisedAsCspWithIdentifierHeaders) =>
        maybeAuthorisedAsCspWithIdentifierHeaders.fold{
          customsAuthService.authAsNonCsp.map[Either[Result, AuthorisedRequest[A]]]{
            case Left(errorResponse) =>
              Left(errorResponse.XmlResult.withConversationId)
            case Right(nonCspData) =>
              Right(vhr.toNonCspAuthorisedRequest(nonCspData.eori))
          }
        }{ cspData =>
          Future.successful(Right(vhr.toCspAuthorisedRequest(cspData)))
        }
      case Left(result) =>
        Future.successful(Left(result.XmlResult.withConversationId))
    }
  }

  private def authAsCspWithMandatoryAuthHeaders[A](implicit vhr: HasRequest[A] with HasConversationId, hc: HeaderCarrier): Future[Either[ErrorResponse, Option[AuthorisedAsCsp]]] = {

    val eventualAuthWithIdentifierHeaders: Future[Either[ErrorResponse, Option[AuthorisedAsCsp]]] =
      customsAuthService.authAsCsp.map {
        case Right(isCsp) =>
          if (isCsp) {
            eitherCspAuthData.right.map(authAsCsp => Some(authAsCsp))
          } else {
            Right(None)
          }
        case Left(errorResponse) =>
          Left(errorResponse)
      }
    eventualAuthWithIdentifierHeaders
  }

  def eitherCspAuthData[A](implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, AuthorisedAsCsp] = {
    val cpsAuth: Either[ErrorResponse, Csp] = for {
      maybeBadgeId <- eitherBadgeIdentifier(allowNone = true).right
      maybeEori <- eitherEori.right
    } yield Csp(maybeEori, maybeBadgeId)

    if (cpsAuth.isRight && cpsAuth.right.get.badgeIdentifier.isEmpty && cpsAuth.right.get.eori.isEmpty) {
      logger.error(s"Both $XSubmitterIdentifierHeaderName and $XBadgeIdentifierHeaderName are missing")
      Left(errorResponseMissingIdentifiers)
    } else {
      cpsAuth
    }
  }

  private def eitherEori[A](implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[Eori]] = {
    headerValidator.eoriMustBeValidIfPresent
  }

  protected def eitherBadgeIdentifier[A](allowNone: Boolean)(implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[BadgeIdentifier]] = {
    headerValidator.eitherBadgeIdentifier(allowNone = allowNone)
  }

}
