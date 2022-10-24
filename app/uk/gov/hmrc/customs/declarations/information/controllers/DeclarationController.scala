/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declarations.information.controllers

import play.api.http.Status.BAD_REQUEST
import play.api.mvc._
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declarations.information.logging.InformationLogger
import uk.gov.hmrc.customs.declarations.information.model._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declarations.information.model.actionbuilders.AuthorisedRequest

trait DeclarationController {

  def validateMrn(mrn: Mrn, logger: InformationLogger)(implicit asr: AuthorisedRequest[AnyContent]): Either[Result, Unit] = {
    if(mrn.validValue) {
      Right((): Unit)
    } else {
      val appropriateResponse = if (mrn.valueTooLong) {
        logger.info(s"MRN parameter is too long: $mrn")
        ErrorResponse(BAD_REQUEST, BadRequestCode, "MRN parameter too long")
      } else if (mrn.valueTooShort) {
        logger.info(s"MRN parameter is missing: $mrn")
        ErrorResponse(BAD_REQUEST, BadRequestCode, "Missing MRN parameter")
      } else {
        logger.info(s"MRN parameter is invalid: $mrn")
        ErrorResponse(BAD_REQUEST, "CDS60002", "MRN parameter invalid")
      }
      val response = appropriateResponse.XmlResult.withConversationId
      logger.debug(s"Full declaration MRN parameter validation failed sending response: $response")
      Left(response)
    }
  }

}
