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

import play.api.mvc._
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.{AuthorisedRequest, FullDeclarationRequest}
import uk.gov.hmrc.customs.declarations.information.services.CustomsAuthService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left

/** Action builder that attempts to authorise request as a CSP or else NON CSP
 * <ul>
 * <li/>INPUT - `FullDeclarationRequest`
 * <li/>OUTPUT - `AuthorisedRequest` - authorised will be `AuthorisedAs.Csp` or `AuthorisedAs.NonCsp`
 * <li/>ERROR -
 * <ul>
 * <li/>400 if authorised as CSP but both badge identifier and submitter identifier not present for CSP
 * <li/>401 if authorised as non-CSP but enrolments does not contain an EORI.
 * <li/>401 if not authorised as CSP or non-CSP
 * <li/>500 on any downstream errors returning 500
 * </ul>
 */
@Singleton
class FullDeclarationAuthAction @Inject()(customsAuthService: CustomsAuthService,
                                          headerValidator: HeaderValidator,
                                          logger: InformationLogger)
                                         (implicit ec: ExecutionContext)
  extends AuthAction(customsAuthService, headerValidator, logger) with ActionRefiner[FullDeclarationRequest, AuthorisedRequest] {

  override protected[this] def executionContext: ExecutionContext = ec

  override def refine[A](fdvr: FullDeclarationRequest[A]): Future[Either[Result, AuthorisedRequest[A]]] = {
    implicit val implicitVhr: FullDeclarationRequest[A] = fdvr
    implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromRequest(rh)

    authAsCspWithMandatoryAuthHeaders.flatMap{
      case Right(maybeAuthorisedAsCspWithIdentifierHeaders) =>
        maybeAuthorisedAsCspWithIdentifierHeaders.fold{
          customsAuthService.authAsNonCsp.map[Either[Result, AuthorisedRequest[A]]]{
            case Left(errorResponse) =>
              Left(errorResponse.XmlResult.withConversationId)
            case Right(nonCspData) =>
              Right(fdvr.toNonCspAuthorisedRequest(nonCspData.eori))
          }
        }{ cspData =>
          Future.successful(Right(fdvr.toCspAuthorisedRequest(cspData)))
        }
      case Left(result) =>
        Future.successful(Left(result.XmlResult.withConversationId))
    }
  }

}
