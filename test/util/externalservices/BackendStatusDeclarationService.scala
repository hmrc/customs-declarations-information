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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.UrlPattern
import org.joda.time.{DateTime, DateTimeZone}
import play.api.test.Helpers._
import util.StatusTestXMLData.validBackendStatusResponse
import util._

import scala.xml.NodeSeq

trait BackendStatusDeclarationService extends WireMockRunner {
  private val urlV1 = urlMatching(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV1)
  private val urlV2 = urlMatching(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV2)

  val acceptanceDateVal = DateTime.now(DateTimeZone.UTC).minusDays(30)

  def startBackendStatusServiceV1(status: Int = OK, body: NodeSeq = validBackendStatusResponse): Unit = {
    startBackendStatusService(urlV1, status, body)
  }

  def startBackendStatusServiceV2(status: Int = OK, body: NodeSeq = validBackendStatusResponse): Unit = {
    startBackendStatusService(urlV2, status, body)
  }

  private def startBackendStatusService(url: UrlPattern, status: Int, body: NodeSeq): Unit = {
    stubFor(post(url).
      willReturn(
        aResponse()
          .withStatus(status)
          .withBody(body.toString())))
  }

  def verifyBackendStatusDecServiceWasCalledWith(requestBody: String,
                                                 expectedAuthToken: String = ExternalServicesConfig.AuthToken,
                                                 maybeUnexpectedAuthToken: Option[String] = None) {
    verify(1, postRequestedFor(urlV1)
      .withHeader(CONTENT_TYPE, equalTo(XML + "; charset=utf-8"))
      .withHeader(ACCEPT, equalTo(XML))
      .withHeader(AUTHORIZATION, equalTo(s"Bearer $expectedAuthToken"))
      .withHeader(DATE, notMatching(""))
      .withHeader("X-Correlation-ID", notMatching(""))
      .withHeader(X_FORWARDED_HOST, equalTo("MDTP"))
      .withRequestBody(equalToXml(requestBody))
      )

    maybeUnexpectedAuthToken foreach { unexpectedAuthToken =>
      verify(0, postRequestedFor(urlV1).withHeader(AUTHORIZATION, equalTo(s"Bearer $unexpectedAuthToken")))
    }
  }
}
