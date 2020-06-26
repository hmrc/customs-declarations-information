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

package component

import java.time.{Instant, ZoneId, ZonedDateTime}

import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.customs.declarations.information.services.{DateTimeService, UniqueIdsService}
import util.{CustomsDeclarationsExternalServicesConfig, ExternalServicesConfig}
import util.TestData.stubUniqueIdsService

trait ComponentTestSpec extends FeatureSpec
    with GivenWhenThen
    with GuiceOneAppPerSuite
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Eventually
    with MockitoSugar {

  private val mockDateTimeService =  mock[DateTimeService]

  val dateTime = 1546344000000L // 01/01/2019 12:00:00

  when(mockDateTimeService.nowUtc()).thenReturn(new DateTime(dateTime, DateTimeZone.UTC))
  when(mockDateTimeService.zonedDateTimeUtc).thenReturn(ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateTime), ZoneId.of("UTC")))

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(bind[DateTimeService].toInstance(mockDateTimeService))
    .overrides(bind[UniqueIdsService].toInstance(stubUniqueIdsService))
    .configure(Map(
    "xml.max-errors" -> 2,
    "microservice.services.auth.host" -> ExternalServicesConfig.Host,
    "microservice.services.auth.port" -> ExternalServicesConfig.Port,
    "microservice.services.declaration-status.host" -> ExternalServicesConfig.Host,
    "microservice.services.declaration-status.port" -> ExternalServicesConfig.Port,
    "microservice.services.declaration-status.context" -> CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV1,
    "microservice.services.declaration-status.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.v2.declaration-status.host" -> ExternalServicesConfig.Host,
    "microservice.services.v2.declaration-status.port" -> ExternalServicesConfig.Port,
    "microservice.services.v2.declaration-status.context" -> CustomsDeclarationsExternalServicesConfig.BackendStatusDeclarationServiceContextV2,
    "microservice.services.v2.declaration-status.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.api-subscription-fields.host" -> ExternalServicesConfig.Host,
    "microservice.services.api-subscription-fields.port" -> ExternalServicesConfig.Port,
    "microservice.services.api-subscription-fields.context" -> CustomsDeclarationsExternalServicesConfig.ApiSubscriptionFieldsContext,
    "auditing.enabled" -> false,
    "auditing.consumer.baseUri.host" -> ExternalServicesConfig.Host,
    "auditing.consumer.baseUri.port" -> ExternalServicesConfig.Port
  )).build()

}