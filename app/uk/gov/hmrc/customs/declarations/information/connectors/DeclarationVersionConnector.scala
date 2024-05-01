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

package uk.gov.hmrc.customs.declarations.information.connectors

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declarations.information.config.ConfigService
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.xml.BackendVersionPayloadCreator
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext

@Singleton
class DeclarationVersionConnector @Inject()(http: HttpClient,
                                            logger: InformationLogger,
                                            backendPayloadCreator: BackendVersionPayloadCreator,
                                            serviceConfigProvider: ServiceConfigProvider,
                                            config: ConfigService,
                                            override val cdsLogger: CdsLogger,
                                            override val actorSystem: ActorSystem)(implicit override val ec: ExecutionContext)
  extends AbstractDeclarationConnector(http, logger, backendPayloadCreator, serviceConfigProvider, config) {
  override lazy val numberOfCallsToTriggerStateChange: Int = config.informationCircuitBreakerConfig.numberOfCallsToTriggerStateChange
  override lazy val unstablePeriodDurationInMillis: Int = config.informationCircuitBreakerConfig.unstablePeriodDurationInMillis
  override lazy val unavailablePeriodDurationInMillis: Int = config.informationCircuitBreakerConfig.unavailablePeriodDurationInMillis
  override val configKey = "declaration-version"
}
