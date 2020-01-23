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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.test.Helpers._
import util.StatusTestXMLData.validBackendStatusResponse
import util._

import scala.xml.NodeSeq

trait BackendStatusDeclarationService extends WireMockRunner {
  private val url = urlMatching(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContext)

  val acceptanceDateVal = DateTime.now(DateTimeZone.UTC).minusDays(30)

  def startBackendStatusService(status: Int = OK, body: NodeSeq = validBackendStatusResponse): Unit = {
    stubFor(post(url).
      willReturn(
        aResponse()
          .withStatus(status)
          .withBody(body.toString())))
  }

  def verifyBackendStatusDecServiceWasCalledWith(requestBody: String,
                                                 expectedAuthToken: String = ExternalServicesConfig.AuthToken,
                                                 maybeUnexpectedAuthToken: Option[String] = None) {
    verify(1, postRequestedFor(url)
      .withHeader(CONTENT_TYPE, equalTo(XML + "; charset=utf-8"))
      .withHeader(ACCEPT, equalTo(XML))
      .withHeader(AUTHORIZATION, equalTo(s"Bearer $expectedAuthToken"))
      .withHeader(DATE, notMatching(""))
      .withHeader("X-Correlation-ID", notMatching(""))
      .withHeader(X_FORWARDED_HOST, equalTo("MDTP"))
      .withRequestBody(equalToXml(requestBody))
      )

    maybeUnexpectedAuthToken foreach { unexpectedAuthToken =>
      verify(0, postRequestedFor(url).withHeader(AUTHORIZATION, equalTo(s"Bearer $unexpectedAuthToken")))
    }
  }
}