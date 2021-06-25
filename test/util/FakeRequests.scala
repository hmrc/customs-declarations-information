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

package util

import play.api.http.HeaderNames.AUTHORIZATION
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.POST
import util.RequestHeaders._
import util.TestData.{cspBearerToken, nonCspBearerToken}

object FakeRequests {

  lazy val ValidCspDeclarationRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(ValidHeaders.toSeq: _*).fromCsp
  lazy val ValidCspDeclarationVersionRequestWithDeclarationSubmissionChannel: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/mrn/:mrn/version?declarationSubmissionChannel=AuthenticatedPartyOnly").withHeaders(ValidHeaders.toSeq: _*).fromCsp
  lazy val ValidCspDeclarationVersionRequestWithInvalidDeclarationSubmissionChannel: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/mrn/:mrn/version?declarationSubmissionChannel=AuthenticatedPartyOnly1").withHeaders(ValidHeaders.toSeq: _*).fromCsp
  lazy val ValidNonCspDeclarationRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(ValidHeaders.toSeq: _*).fromNonCsp
  lazy val ValidNonCspDeclarationVersionRequestWithDeclarationSubmissionChannel: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/mrn/:mrn/version?declarationSubmissionChannel=AuthenticatedPartyOnly").withHeaders(ValidHeaders.toSeq: _*).fromNonCsp
  lazy val ValidNonCspDeclarationVersionRequestWithInvalidDeclarationSubmissionChannel: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/mrn/:mrn/version?declarationSubmissionChannel=AuthenticatedPartyOnly1").withHeaders(ValidHeaders.toSeq: _*).fromNonCsp

  implicit class FakeRequestOps[R](val fakeRequest: FakeRequest[R]) extends AnyVal {
    def fromCsp: FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $cspBearerToken")

    def fromNonCsp: FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $nonCspBearerToken")

    def withCustomToken(token: String): FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $token")

    def postTo(endpoint: String): FakeRequest[R] = fakeRequest.withMethod(POST).withTarget(fakeRequest.target.withPath(endpoint))
  }

}
