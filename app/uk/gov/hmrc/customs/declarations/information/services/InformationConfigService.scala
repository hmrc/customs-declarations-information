/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declarations.information.services

import cats.implicits._
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.api.common.config.{ConfigValidatedNelAdaptor, CustomsValidatedNel}
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model.{InformationCircuitBreakerConfig, InformationConfig}

@Singleton
class InformationConfigService @Inject()(configValidatedNel: ConfigValidatedNelAdaptor, logger: InformationLogger) {

  private val root = configValidatedNel.root
  private val apiSubscriptionFieldsService = configValidatedNel.service("api-subscription-fields")

  private val numberOfCallsToTriggerStateChangeNel = root.int("circuitBreaker.numberOfCallsToTriggerStateChange")
  private val unavailablePeriodDurationInMillisNel = root.int("circuitBreaker.unavailablePeriodDurationInMillis")
  private val unstablePeriodDurationInMillisNel = root.int("circuitBreaker.unstablePeriodDurationInMillis")

  private val declarationStatusRequestDaysLimit = root.int("declarationStatus.requestDaysLimit")

  private val apiSubscriptionFieldsServiceUrlNel = apiSubscriptionFieldsService.serviceUrl

  private val validatedInformationConfig: CustomsValidatedNel[InformationConfig] = (
    apiSubscriptionFieldsServiceUrlNel, declarationStatusRequestDaysLimit
  ) mapN InformationConfig

  private val validatedDeclarationsCircuitBreakerConfig: CustomsValidatedNel[InformationCircuitBreakerConfig] = (
    numberOfCallsToTriggerStateChangeNel, unavailablePeriodDurationInMillisNel, unstablePeriodDurationInMillisNel
  ) mapN InformationCircuitBreakerConfig

  private val customsConfigHolder =
    (validatedInformationConfig,
      validatedDeclarationsCircuitBreakerConfig
    ) mapN CustomsConfigHolder fold(
      fe = { nel =>
        // error case exposes nel (a NotEmptyList)
        val errorMsg = nel.toList.mkString("\n", "\n", "")
        logger.errorWithoutRequestContext(errorMsg)
        throw new IllegalStateException(errorMsg)
      },
      fa = identity
    )

  val declarationsConfig: InformationConfig = customsConfigHolder.declarationsConfig

  val declarationsCircuitBreakerConfig: InformationCircuitBreakerConfig = customsConfigHolder.declarationsCircuitBreakerConfig

  private case class CustomsConfigHolder(declarationsConfig: InformationConfig,
                                         declarationsCircuitBreakerConfig: InformationCircuitBreakerConfig)
}
