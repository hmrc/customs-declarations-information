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
import play.api.http.Status
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, PrivilegedApplication}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.customs.declarations.information.model.Eori
import util.TestData.{cspBearerToken, declarantEori, nonCspBearerToken}

trait AuthService {

  val authUrl = "/auth/authorise"
  private val authUrlMatcher = urlEqualTo(authUrl)

  private val cspAuthorisationPredicate = Enrolment("write:customs-declarations-information") and AuthProviders(PrivilegedApplication)
  private val nonCspAuthorisationPredicate = Enrolment("HMRC-CUS-ORG") and AuthProviders(GovernmentGateway)
  private val nonCspRetrieval = Retrievals.authorisedEnrolments

  private def bearerTokenMatcher(bearerToken: String)= equalTo("Bearer " + bearerToken)

  private def authRequestJson(predicate: Predicate, retrievals: Retrieval[_]*): String = {
    val predicateJsArray = predicateToJson(predicate)
    val js =
      s"""
         |{
         |  "authorise": $predicateJsArray,
         |  "retrieve": [${retrievals.flatMap(_.propertyNames).map(Json.toJson(_)).mkString(",")}]
         |}
    """.stripMargin
    js
  }

  private def predicateToJson(predicate: Predicate) = {
    predicate.toJson match {
      case arr: JsArray => arr
      case other => Json.arr(other)
    }
  }

  private def authRequestJsonWithAuthorisedEnrolmentRetrievals(predicate: Predicate) = {
    val predicateJsArray: JsArray = predicateToJson(predicate)
    val js =
      s"""
         |{
         |  "authorise": $predicateJsArray,
         |  "retrieve" : ["authorisedEnrolments"]
         |}
    """.stripMargin
    js
  }

  def authServiceAuthorizesCSP(bearerToken: String = cspBearerToken): Unit = {
    stubFor(post(authUrlMatcher)
      .withRequestBody(equalToJson(authRequestJson(cspAuthorisationPredicate)))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody("{}")
      )
    )
  }

  def authServiceAuthorizesNonCspWithEori(bearerToken: String = nonCspBearerToken,
                                          eori: Eori = declarantEori): Unit = {
    stubFor(post(authUrlMatcher)
      .withRequestBody(equalToJson(authRequestJsonWithAuthorisedEnrolmentRetrievals(nonCspAuthorisationPredicate)))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(
            s"""{
               |  "internalId": "Int-d67e2592-e560-4766-9e2a-bd2e107ab50a",
               |  "externalId": "Ext-9cf74a8d-64eb-4ec1-83c1-e432ffa4aa65",
               |  "agentCode": "123456789",
               |  "credentials": {
               |    "providerId": "a-cred-id",
               |    "providerType": "GovernmentGateway"
               |  },
               |  "confidenceLevel": 50,
               |  "name": {
               |    "name": "TestUser"
               |  },
               |  "email": "",
               |  "agentInformation": {},
               |  "groupIdentifier": "testGroupId-af271a17-319f-4d3a-82bc-961f7980d58b",
               |  "mdtpInformation": {
               |    "deviceId": "device-identifier-1234",
               |    "sessionId": "session-id-12345"
               |  },
               |  "itmpName": {},
               |  "itmpAddress": {},
               |  "affinityGroup": "Individual",
               |  "credentialStrength": "strong",
               |  "authorisedEnrolments": [ ${enrolmentRetrievalJson("HMRC-CUS-ORG", "EORINumber", eori.value)} ],
               |  "loginTimes": {
               |    "currentLogin": "2018-04-23T09:26:45.069Z",
               |    "previousLogin": "2018-04-05T13:59:54.082Z"
               |  }
               |}""".stripMargin
          )
      )
    )
  }

  def verifyAuthServiceCalledForCsp(bearerToken: String = cspBearerToken): Unit = {
    verify(1, postRequestedFor(authUrlMatcher)
      .withRequestBody(equalToJson(authRequestJson(cspAuthorisationPredicate)))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
    )
  }

  def verifyAuthServiceCalledForNonCsp(bearerToken: String = nonCspBearerToken): Unit = {
    verify(1, postRequestedFor(authUrlMatcher)
      .withRequestBody(equalToJson(authRequestJson(nonCspAuthorisationPredicate, nonCspRetrieval)))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
    )
  }

  private def enrolmentRetrievalJson(enrolmentKey: String,
                                     identifierName: String,
                                     identifierValue: String): String = {
    s"""
       |{
       | "key": "$enrolmentKey",
       | "identifiers": [
       |   {
       |     "key": "$identifierName",
       |     "value": "$identifierValue"
       |   }
       | ]
       |}
    """.stripMargin
  }

  private def authRequestJsonWithoutRetrievals(predicate: Predicate): String = {
    val predicateJsArray = predicateToJson(predicate)
    val js =
      s"""
         |{
         |  "authorise": $predicateJsArray,
         |  "retrieve" : [ ]
         |}
    """.stripMargin
    js
  }

  private def cspAuthServiceUnauthorisesScope(bearerToken: String, body: String): Unit = {
    stubFor(post(authUrlMatcher)
      .withRequestBody(equalToJson(body))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
      .willReturn(
        aResponse()
          .withStatus(Status.UNAUTHORIZED)
          .withHeader(WWW_AUTHENTICATE, """MDTP detail="InsufficientEnrolments"""")
      )
    )
  }

  def authServiceUnauthorisesScopeForCSPWithoutRetrievals(bearerToken: String = cspBearerToken): Unit = {
    cspAuthServiceUnauthorisesScope(bearerToken, authRequestJsonWithoutRetrievals(cspAuthorisationPredicate))
  }

}
