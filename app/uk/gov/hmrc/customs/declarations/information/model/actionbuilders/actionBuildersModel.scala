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

package uk.gov.hmrc.customs.declarations.information.model.actionbuilders

import play.api.mvc.{Request, Result, WrappedRequest}
import uk.gov.hmrc.customs.declarations.information.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declarations.information.model._

import java.util.Date

object ActionBuilderModelHelper {
  implicit class AddConversationId(val result: Result) extends AnyVal {
    def withConversationId(implicit c: HasConversationId): Result = {
      result.withHeaders(XConversationIdHeaderName -> c.conversationId.toString)
    }
  }

  implicit class ApiVersionRequestOps[A](val avr: ApiVersionRequest[A]) extends AnyVal {
    def toValidatedHeadersRequest(eh: ExtractedHeaders): ValidatedHeadersRequest[A] = ValidatedHeadersRequest(
      avr.conversationId,
      avr.requestedApiVersion,
      eh.clientId,
      avr.request
    )
  }

  implicit class ValidatedHeadersRequestOps[A](val vhr: ValidatedHeadersRequest[A]) {

    def toCspAuthorisedRequest(a: AuthorisedAsCsp): AuthorisedRequest[A] = toAuthorisedRequest(a)

    def toNonCspAuthorisedRequest(eori: Eori): AuthorisedRequest[A] = toAuthorisedRequest(NonCsp(eori))

    def toInternalClientIdsRequest(declarationSubmissionChannel: Option[DeclarationSubmissionChannel]): InternalClientIdsRequest[A] = InternalClientIdsRequest(
      vhr.conversationId,
      vhr.requestedApiVersion,
      vhr.clientId,
      declarationSubmissionChannel,
      vhr.request
    )

    private def toAuthorisedRequest(authorisedAs: AuthorisedAs): AuthorisedRequest[A] = AuthorisedRequest(
      vhr.conversationId,
      vhr.requestedApiVersion,
      vhr.clientId,
      None,
      None,
      None,
      authorisedAs,
      vhr.request
    )
  }

  implicit class InternalClientIdsRequestOps[A](val icir: InternalClientIdsRequest[A]) {
    def toCspAuthorisedRequest(a: AuthorisedAsCsp): AuthorisedRequest[A] = toAuthorisedRequest(a)

    def toNonCspAuthorisedRequest(eori: Eori): AuthorisedRequest[A] = toAuthorisedRequest(NonCsp(eori))

    def toSearchParametersRequest(searchParameters: Option[SearchParameters]): SearchParametersRequest[A] = SearchParametersRequest(
      icir.conversationId,
      icir.requestedApiVersion,
      icir.clientId,
      icir.declarationSubmissionChannel,
      searchParameters,
      icir.request
    )

    def toDeclarationFullRequest(declarationVersion: Option[Int]): DeclarationFullRequest[A] = DeclarationFullRequest(
      icir.conversationId,
      icir.requestedApiVersion,
      icir.clientId,
      icir.declarationSubmissionChannel,
      declarationVersion,
      icir.request
    )

    private def toAuthorisedRequest(authorisedAs: AuthorisedAs): AuthorisedRequest[A] = AuthorisedRequest(
      icir.conversationId,
      icir.requestedApiVersion,
      icir.clientId,
      icir.declarationSubmissionChannel,
      None,
      None,
      authorisedAs,
      icir.request
    )
  }

  implicit class DeclarationFullRequestOps[A](val fdvr: DeclarationFullRequest[A]) {
    def toCspAuthorisedRequest(a: AuthorisedAsCsp): AuthorisedRequest[A] = toAuthorisedRequest(a)

    def toNonCspAuthorisedRequest(eori: Eori): AuthorisedRequest[A] = toAuthorisedRequest(NonCsp(eori))

    private def toAuthorisedRequest(authorisedAs: AuthorisedAs): AuthorisedRequest[A] = AuthorisedRequest(
      fdvr.conversationId,
      fdvr.requestedApiVersion,
      fdvr.clientId,
      fdvr.declarationSubmissionChannel,
      None,
      fdvr.declarationVersion,
      authorisedAs,
      fdvr.request
    )
  }

  implicit class SearchParametersRequestOps[A](val spr: SearchParametersRequest[A]) {
    def toCspAuthorisedRequest(a: AuthorisedAsCsp): AuthorisedRequest[A] = toAuthorisedRequest(a)

    def toNonCspAuthorisedRequest(eori: Eori): AuthorisedRequest[A] = toAuthorisedRequest(NonCsp(eori))

    private def toAuthorisedRequest(authorisedAs: AuthorisedAs): AuthorisedRequest[A] = AuthorisedRequest(
      spr.conversationId,
      spr.requestedApiVersion,
      spr.clientId,
      spr.declarationSubmissionChannel,
      spr.searchParameters,
      None,
      authorisedAs,
      spr.request
    )
  }
}

trait HasRequest[A] {
  val request: Request[A]
}

trait HasConversationId {
  val conversationId: ConversationId
}

trait HasApiVersion {
  val requestedApiVersion: ApiVersion
}

trait ExtractedHeaders {
  val clientId: ClientId
}

trait HasAuthorisedAs {
  val authorisedAs: AuthorisedAs
}

trait HasBadgeIdentifier {
  val badgeIdentifier: BadgeIdentifier
}

trait HasDeclarationSubmissionChannel {
  val declarationSubmissionChannel: Option[DeclarationSubmissionChannel]
}

trait HasSearchParameters {
  val searchParameters: Option[SearchParameters]
}

trait HasDeclarationVersion {
  val declarationVersion: Option[Int]
}

case class ExtractedHeadersImpl(clientId: ClientId) extends ExtractedHeaders

/*
 * We need multiple WrappedRequest classes to reflect additions to context during the request processing pipeline.
 *
 * There is some repetition in the WrappedRequest classes, but the benefit is we get a flat structure for our data
 * items, reducing the number of case classes and making their use much more convenient, rather than deeply nested stuff
 * eg `r.badgeIdentifier` vs `r.requestData.badgeIdentifier`
 */

case class ConversationIdRequest[A](conversationId: ConversationId,
                                    request: Request[A]
) extends WrappedRequest[A](request) with HasRequest[A] with HasConversationId

// Available after ShutterCheckAction
case class ApiVersionRequest[A](conversationId: ConversationId,
                                requestedApiVersion: ApiVersion,
                                request: Request[A]
) extends WrappedRequest[A](request) with HasRequest[A] with HasConversationId with HasApiVersion

// Available after ValidatedHeadersAction builder
case class ValidatedHeadersRequest[A](conversationId: ConversationId,
                                      requestedApiVersion: ApiVersion,
                                      clientId: ClientId,
                                      request: Request[A]
) extends WrappedRequest[A](request) with HasRequest[A] with HasConversationId with ExtractedHeaders with HasApiVersion

// Available after InternalClientIdsCheckAction
case class InternalClientIdsRequest[A](conversationId: ConversationId,
                                       requestedApiVersion: ApiVersion,
                                       clientId: ClientId,
                                       declarationSubmissionChannel: Option[DeclarationSubmissionChannel],
                                       request: Request[A]
) extends WrappedRequest[A](request) with HasRequest[A] with HasConversationId with HasApiVersion with ExtractedHeaders with HasDeclarationSubmissionChannel

// Available after DeclarationFullCheckAction
case class DeclarationFullRequest[A](conversationId: ConversationId,
                                     requestedApiVersion: ApiVersion,
                                     clientId: ClientId,
                                     declarationSubmissionChannel: Option[DeclarationSubmissionChannel],
                                     declarationVersion: Option[Int],
                                     request: Request[A]
                                      ) extends WrappedRequest[A](request) with HasRequest[A] with HasConversationId with HasApiVersion with ExtractedHeaders with HasDeclarationSubmissionChannel with HasDeclarationVersion

// Available after SearchParametersCheckAction
case class SearchParametersRequest[A](conversationId: ConversationId,
                                      requestedApiVersion: ApiVersion,
                                      clientId: ClientId,
                                      declarationSubmissionChannel: Option[DeclarationSubmissionChannel],
                                      searchParameters: Option[SearchParameters],
                                      request: Request[A]
                                      ) extends WrappedRequest[A](request) with HasRequest[A] with HasConversationId with HasApiVersion with ExtractedHeaders with HasDeclarationSubmissionChannel with HasSearchParameters {

}

case class SearchParameters(eori: Option[Eori],
                            partyRole: PartyRole,
                            declarationCategory: DeclarationCategory,
                            goodsLocationCode: Option[GoodsLocationCode],
                            declarationStatus: Option[DeclarationStatus],
                            dateFrom: Option[Date],
                            dateTo: Option[Date],
                            pageNumber: Option[Int])

// Available after AuthAction builder
case class AuthorisedRequest[A](conversationId: ConversationId,
                                requestedApiVersion: ApiVersion,
                                clientId: ClientId,
                                declarationSubmissionChannel: Option[DeclarationSubmissionChannel],
                                searchParameters: Option[SearchParameters],
                                declarationVersion: Option[Int],
                                authorisedAs: AuthorisedAs,
                                request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with ExtractedHeaders with HasAuthorisedAs with HasApiVersion with HasSearchParameters with HasDeclarationSubmissionChannel with HasDeclarationVersion
