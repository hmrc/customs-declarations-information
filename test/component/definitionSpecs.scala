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

package component

import org.scalatest.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class DefinitionSpecWithWhitelistedAppId extends ComponentTestSpec
  with Matchers {

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(Map(
    "api.access.version-1.0.whitelistedApplicationIds.0" -> "someId-1",
    "api.access.version-2.0.whitelistedApplicationIds.0" -> "someId-2"
  )).build()

  feature("Ensure definition file") {

    scenario("is correct when there are whitelisted applicationIds") {

      Given("the API is available")
      val request = FakeRequest("GET", "/api/definition")

      When("api definition is requested")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 200 status is received")
      val resultFuture = result.get
      status(resultFuture) shouldBe OK

      And("the response body is correct")
      contentAsJson(resultFuture) shouldBe Json.parse(
        """
          |{
          |  "scopes": [
          |    {
          |      "key": "write:customs-declarations-information",
          |      "name": "Request the status of a declaration",
          |      "description": "Request the status of a declaration"
          |    }
          |  ],
          |  "api": {
          |    "name": "Customs Declarations Information",
          |    "description": "Retrieve declaration status information",
          |    "context": "customs/declarations-information",
          |    "categories": ["CUSTOMS"],
          |    "versions": [
          |      {
          |        "version": "1.0",
          |        "status": "BETA",
          |        "endpointsEnabled": true,
          |        "access": {
          |          "type": "PRIVATE",
          |          "whitelistedApplicationIds": [
          |          "someId-1"
          |          ]
          |        },
          |        "fieldDefinitions": [
          |          {
          |            "name": "authenticatedEori",
          |            "description": "What's your Economic Operator Registration and Identification (EORI) number?",
          |            "type": "STRING",
          |            "hint": "This is your EORI that will associate your application with you as a CSP",
          |            "shortDescription" : "EORI"
          |          }
          |        ]
          |      },
          |      {
          |        "version": "2.0",
          |        "status": "BETA",
          |        "endpointsEnabled": true,
          |        "access": {
          |          "type": "PRIVATE",
          |          "whitelistedApplicationIds": [
          |          "someId-2"
          |          ]
          |        },
          |        "fieldDefinitions": [
          |          {
          |            "name": "authenticatedEori",
          |            "description": "What's your Economic Operator Registration and Identification (EORI) number?",
          |            "type": "STRING",
          |            "hint": "This is your EORI that will associate your application with you as a CSP",
          |            "shortDescription" : "EORI"
          |          }
          |        ]
          |      }
          |    ]
          |  }
          |}
        """.stripMargin)
    }
  }
}

class DefinitionSpecWithoutWhitelistedAppId extends ComponentTestSpec
  with Matchers {

  override implicit lazy val app: Application = new GuiceApplicationBuilder().build()

  feature("Ensure definition file") {

    scenario("is correct when there are no whitelisted applicationIds") {

      Given("the API is available")
      val request = FakeRequest("GET", "/api/definition")

      When("api definition is requested")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 200 status is received")
      val resultFuture = result.get
      status(resultFuture) shouldBe OK

      And("the response body is correct")
      contentAsJson(resultFuture) shouldBe Json.parse(
        """
          |{
          |  "scopes": [
          |    {
          |      "key": "write:customs-declarations-information",
          |      "name": "Request the status of a declaration",
          |      "description": "Request the status of a declaration"
          |    }
          |  ],
          |  "api": {
          |    "name": "Customs Declarations Information",
          |    "description": "Retrieve declaration status information",
          |    "context": "customs/declarations-information",
          |    "categories": ["CUSTOMS"],
          |    "versions": [
          |      {
          |        "version": "1.0",
          |        "status": "BETA",
          |        "endpointsEnabled": true,
          |        "access": {
          |          "type": "PUBLIC"
          |        },
          |        "fieldDefinitions": [
          |          {
          |            "name": "authenticatedEori",
          |            "description": "What's your Economic Operator Registration and Identification (EORI) number?",
          |            "type": "STRING",
          |            "hint": "This is your EORI that will associate your application with you as a CSP",
          |            "shortDescription" : "EORI"
          |          }
          |        ]
          |      },
          |      {
          |        "version": "2.0",
          |        "status": "BETA",
          |        "endpointsEnabled": true,
          |        "access": {
          |          "type": "PUBLIC"
          |        },
          |        "fieldDefinitions": [
          |          {
          |            "name": "authenticatedEori",
          |            "description": "What's your Economic Operator Registration and Identification (EORI) number?",
          |            "type": "STRING",
          |            "hint": "This is your EORI that will associate your application with you as a CSP",
          |            "shortDescription" : "EORI"
          |          }
          |        ]
          |      }
          |    ]
          |  }
          |}
        """.stripMargin)
    }
  }
}