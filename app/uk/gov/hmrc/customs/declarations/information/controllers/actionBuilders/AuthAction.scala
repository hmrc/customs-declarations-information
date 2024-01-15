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

package uk.gov.hmrc.customs.declarations.information.controllers.actionBuilders

import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.declarations.information.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{HasConversationId, HasRequest}
import uk.gov.hmrc.customs.declarations.information.model.{AuthorisedAsCsp, BadgeIdentifier, Csp, Eori}
import uk.gov.hmrc.customs.declarations.information.services.CustomsAuthService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left

abstract class AuthAction @Inject()(customsAuthService: CustomsAuthService,
                                    headerValidator: HeaderValidator,
                                    logger: InformationLogger)
                                   (implicit ec: ExecutionContext) {
  protected def errorResponseMissingIdentifiers = errorBadRequest(s"Both $XSubmitterIdentifierHeaderName and $XBadgeIdentifierHeaderName headers are missing")

  protected def authAsCspWithMandatoryAuthHeaders[A](implicit vhr: HasRequest[A] with HasConversationId, hc: HeaderCarrier): Future[Either[ErrorResponse, Option[AuthorisedAsCsp]]] = {

    val eventualAuthWithIdentifierHeaders: Future[Either[ErrorResponse, Option[AuthorisedAsCsp]]] =
      customsAuthService.authAsCsp.map {
        case Right(isCsp) =>
          if (isCsp) {
            eitherCspAuthData.map(authAsCsp => Some(authAsCsp))
          } else {
            Right(None)
          }
        case Left(errorResponse) =>
          Left(errorResponse)
      }
    eventualAuthWithIdentifierHeaders
  }

  protected def eitherCspAuthData[A](implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, AuthorisedAsCsp] = {
    val cpsAuth: Either[ErrorResponse, Csp] = for {
      maybeBadgeId <- eitherBadgeIdentifier(allowNone = true)
      maybeEori <- eitherEori
    } yield Csp(maybeEori, maybeBadgeId)

    if (cpsAuth.isRight && cpsAuth.toOption.get.badgeIdentifier.isEmpty && cpsAuth.toOption.get.eori.isEmpty) {
      logger.error(s"Both $XSubmitterIdentifierHeaderName and $XBadgeIdentifierHeaderName are missing")
      Left(errorResponseMissingIdentifiers)
    } else {
      cpsAuth
    }
  }

  protected def eitherEori[A](implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[Eori]] = {
    headerValidator.eoriMustBeValidIfPresent
  }

  protected def eitherBadgeIdentifier[A](allowNone: Boolean)(implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[BadgeIdentifier]] = {
    headerValidator.eitherBadgeIdentifier(allowNone = allowNone)
  }

}
