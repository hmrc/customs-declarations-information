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

package uk.gov.hmrc.customs.declarations.information.model.actionbuilders

import play.api.mvc.{Request, Result, WrappedRequest}
import uk.gov.hmrc.customs.declarations.information.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declarations.information.model._

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

    private def toAuthorisedRequest(authorisedAs: AuthorisedAs): AuthorisedRequest[A] = AuthorisedRequest(
      vhr.conversationId,
      vhr.requestedApiVersion,
      vhr.clientId,
      authorisedAs,
      vhr.request
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

// Available after AuthAction builder
case class AuthorisedRequest[A](conversationId: ConversationId,
                                requestedApiVersion: ApiVersion,
                                clientId: ClientId,
                                authorisedAs: AuthorisedAs,
                                request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with ExtractedHeaders with HasAuthorisedAs with HasApiVersion
