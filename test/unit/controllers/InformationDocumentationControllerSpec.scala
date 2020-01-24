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

package unit.controllers

import controllers.Assets
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.customs.declarations.information.controllers.InformationDocumentationController
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger

class InformationDocumentationControllerSpec extends PlaySpec with MockitoSugar with Results with BeforeAndAfterEach {

  private val mockService = mock[HttpErrorHandler]

  private val mockLogger = mock[InformationLogger]

  private val v1WhitelistedAppIdsConfigs = Map(
    "api.access.version-1.0.whitelistedApplicationIds.0" -> "v1AppId-1",
    "api.access.version-1.0.whitelistedApplicationIds.1" -> "v1AppId-2")

  private def getApiDefinitionWith(configMap: Map[String, Any]) =
    new InformationDocumentationController(mock[Assets], Helpers.stubControllerComponents(), play.api.Configuration.from(configMap), mockLogger)
      .definition()

  override def beforeEach() {
    reset(mockService)
  }

  "API Definition" should {

    "be correct when V1 & V2 are PUBLIC by default and V3 is always private" in {
      val result = getApiDefinitionWith(Map())(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson(None)
    }

    "be correct when V1 is PRIVATE & V2 is public  and V3 is always private" in {
      val result = getApiDefinitionWith(v1WhitelistedAppIdsConfigs)(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson(expectedV1WhitelistedAppIds = Some(v1WhitelistedAppIdsConfigs.values))
    }

  }

  private def expectedJson(expectedV1WhitelistedAppIds: Option[Iterable[String]]) =
    Json.parse(
      s"""
         |{
         |   "scopes":[
         |      {
         |         "key":"write:customs-declarations-information",
         |         "name":"Request the status of a declaration",
         |         "description":"Request the status of a declaration"
         |      }
         |   ],
         |   "api":{
         |      "name":"Customs Declarations Information",
         |      "description":"Allows CSPs to retrieve declaration status information",
         |      "context":"customs/declarations-information",
         |      "categories": ["CUSTOMS"],
         |      "versions":[
         |         {
         |            "version":"1.0",
         |            "status":"BETA",
         |            "endpointsEnabled":true,
         |            "access":{
         |               """.stripMargin
        +
        expectedV1WhitelistedAppIds.fold(""" "type":"PUBLIC" """)(ids =>
          """ "type":"PRIVATE", "whitelistedApplicationIds":[ """.stripMargin
            + ids.map(x => s""" "$x" """).mkString(",") + "]"
        )
        +
        s"""
           |            }
           |         }
           |      ]
           |   }
           |}
      """.stripMargin)

}
