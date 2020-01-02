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

  implicit class CorrelationIdsRequestOps[A](val cir: ConversationIdRequest[A]) extends AnyVal {
    def toValidatedHeadersRequest(eh: ExtractedHeaders): ValidatedHeadersRequest[A] = ValidatedHeadersRequest(
      cir.conversationId,
      eh.requestedApiVersion,
      eh.badgeIdentifier,
      eh.clientId,
      cir.request
    )
  }

  implicit class ValidatedHeadersRequestOps[A](val vhr: ValidatedHeadersRequest[A]) extends AnyVal {

    def toAuthorisedRequest: AuthorisedRequest[A] = AuthorisedRequest(
      vhr.conversationId,
      vhr.requestedApiVersion,
      vhr.badgeIdentifier,
      vhr.clientId,
      Csp(vhr.badgeIdentifier),
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

trait ExtractedHeaders {
  val requestedApiVersion: ApiVersion
  val clientId: ClientId
  val badgeIdentifier: BadgeIdentifier
}

trait HasAuthorisedAs {
  val authorisedAs: AuthorisedAs
}

case class ExtractedHeadersImpl(requestedApiVersion: ApiVersion,
                                badgeIdentifier: BadgeIdentifier,
                                clientId: ClientId)
  extends ExtractedHeaders

/*
 * We need multiple WrappedRequest classes to reflect additions to context during the request processing pipeline.
 *
 * There is some repetition in the WrappedRequest classes, but the benefit is we get a flat structure for our data
 * items, reducing the number of case classes and making their use much more convenient, rather than deeply nested stuff
 * eg `r.badgeIdentifier` vs `r.requestData.badgeIdentifier`
 */

case class ConversationIdRequest[A](conversationId: ConversationId,
                                    request: Request[A])
  extends WrappedRequest[A](request) with HasRequest[A] with HasConversationId

case class ValidatedHeadersRequest[A](conversationId: ConversationId,
                                      requestedApiVersion: ApiVersion,
                                      badgeIdentifier: BadgeIdentifier,
                                      clientId: ClientId,
                                      request: Request[A])
  extends WrappedRequest[A](request) with HasRequest[A] with HasConversationId with ExtractedHeaders

case class AuthorisedRequest[A](conversationId: ConversationId,
                                requestedApiVersion: ApiVersion,
                                badgeIdentifier: BadgeIdentifier,
                                clientId: ClientId,
                                authorisedAs: AuthorisedAs,
                                request: Request[A])
  extends WrappedRequest[A](request) with HasConversationId with ExtractedHeaders with HasAuthorisedAs
