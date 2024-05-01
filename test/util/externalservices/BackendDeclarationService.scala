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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.UrlPattern
import play.api.test.Helpers._
import util.FullTestXMLData.validBackendFullResponse
import util.SearchTestXMLData.validBackendSearchResponse
import util.StatusTestXMLData.validBackendStatusResponse
import util.VersionTestXMLData.validBackendVersionResponse
import util._

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import scala.xml.NodeSeq

trait BackendDeclarationService extends WireMockRunner {
  private val statusUrlV1 = urlMatching(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV1)
  private val statusUrlV2 = urlMatching(CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV2)
  private val versionUrlV1 = urlMatching(CustomsDeclarationsExternalServicesConfig.BackendVersionDeclarationServiceContextV1)
  private val versionUrlV2 = urlMatching(CustomsDeclarationsExternalServicesConfig.BackendVersionDeclarationServiceContextV2)
  private val searchUrlV1 = urlMatching(CustomsDeclarationsExternalServicesConfig.BackendSearchDeclarationServiceContextV1)
  private val searchUrlV2 = urlMatching(CustomsDeclarationsExternalServicesConfig.BackendSearchDeclarationServiceContextV2)
  private val fullUrlV1 = urlMatching(CustomsDeclarationsExternalServicesConfig.BackendFullDeclarationServiceContextV1)
  private val fullUrlV2 = urlMatching(CustomsDeclarationsExternalServicesConfig.BackendFullDeclarationServiceContextV2)
  val acceptanceDateVal: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.of("UTC")).minusDays(30)

  def startBackendStatusServiceV1(status: Int = OK, body: NodeSeq = validBackendStatusResponse): Unit = {
    startBackendService(statusUrlV1, status, body)
  }

  def startBackendStatusServiceV2(status: Int = OK, body: NodeSeq = validBackendStatusResponse): Unit = {
    startBackendService(statusUrlV2, status, body)
  }

  def startBackendVersionServiceV1(status: Int = OK, body: NodeSeq = validBackendVersionResponse): Unit = {
    startBackendService(versionUrlV1, status, body)
  }

  def startBackendVersionServiceV2(status: Int = OK, body: NodeSeq = validBackendVersionResponse): Unit = {
    startBackendService(versionUrlV2, status, body)
  }

  def startBackendSearchServiceV1(status: Int = OK, body: NodeSeq = validBackendSearchResponse): Unit = {
    startBackendService(searchUrlV1, status, body)
  }

  def startBackendSearchServiceV2(status: Int = OK, body: NodeSeq = validBackendSearchResponse): Unit = {
    startBackendService(searchUrlV2, status, body)
  }

  def startBackendFullServiceV1(status: Int = OK, body: NodeSeq = validBackendFullResponse): Unit = {
    startBackendService(fullUrlV1, status, body)
  }

  def startBackendFullServiceV2(status: Int = OK, body: NodeSeq = validBackendFullResponse): Unit = {
    startBackendService(fullUrlV2, status, body)
  }

  private def startBackendService(url: UrlPattern, status: Int, body: NodeSeq): Unit = {
    stubFor(post(url).
      willReturn(
        aResponse()
          .withStatus(status)
          .withBody(body.toString())))
  }

  def verifyBackendDecServiceWasCalledWith(url: String = CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV1,
                                           requestBody: String,
                                           expectedAuthToken: String = ExternalServicesConfig.AuthToken,
                                           maybeUnexpectedAuthToken: Option[String] = None): Unit = {
    verify(1, postRequestedFor(urlMatching(url))
      .withHeader(CONTENT_TYPE, equalTo(XML + "; charset=utf-8"))
      .withHeader(ACCEPT, equalTo(XML))
      .withHeader(AUTHORIZATION, equalTo(s"Bearer $expectedAuthToken"))
      .withHeader(DATE, notMatching(""))
      .withHeader("X-Correlation-ID", notMatching(""))
      .withHeader(X_FORWARDED_HOST, equalTo("MDTP"))
      .withRequestBody(equalToXml(requestBody))
      )

    maybeUnexpectedAuthToken foreach { unexpectedAuthToken =>
      verify(0, postRequestedFor(statusUrlV1).withHeader(AUTHORIZATION, equalTo(s"Bearer $unexpectedAuthToken")))
    }
  }
}
