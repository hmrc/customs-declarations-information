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

package uk.gov.hmrc.customs.declarations.information.model

import java.util.UUID

import play.api.libs.json._

case class RequestedVersion(versionNumber: String, configPrefix: Option[String])

case class Eori(value: String) extends AnyVal {
  override def toString: String = value.toString
}
object Eori {
  implicit val writer: Writes[Eori] = Writes[Eori] { x => JsString(x.value) }
  implicit val reader: Reads[Eori] = Reads.of[String].map(new Eori(_))
}

case class ClientId(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class ConversationId(uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}
object ConversationId {
  implicit val writer: Writes[ConversationId] = Writes[ConversationId] { x => JsString(x.uuid.toString) }
  implicit val reader: Reads[ConversationId] = Reads.of[UUID].map(new ConversationId(_))
}

case class Mrn(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class CorrelationId(uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}

case class DeclarationManagementInformationRequestId(uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}

case class BadgeIdentifier(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class SubscriptionFieldsId(value: UUID) extends AnyVal{
  override def toString: String = value.toString
}
object SubscriptionFieldsId {
  implicit val writer: Writes[SubscriptionFieldsId] = Writes[SubscriptionFieldsId] { x => JsString(x.value.toString) }
  implicit val reader: Reads[SubscriptionFieldsId] = Reads.of[UUID].map(new SubscriptionFieldsId(_))
}

sealed trait ApiVersion {
  val value: String
  val configPrefix: String
  override def toString: String = value
}
object VersionOne extends ApiVersion{
  override val value: String = "1.0"
  override val configPrefix: String = ""
}

sealed trait AuthorisedAs {
}
case class Csp(badgeIdentifier: BadgeIdentifier) extends AuthorisedAs
case class CspWithEori(badgeIdentifier: BadgeIdentifier, eori: Eori) extends AuthorisedAs

private object NotAvailable { val na = Some("NOT AVAILABLE") }

case class DeclarationStatusResponse(declaration: Declaration)
case class Declaration(versionNumber: Option[String] = NotAvailable.na, creationDate: Option[String] = NotAvailable.na, acceptanceDate: Option[String] = NotAvailable.na,  tradeMovementType: Option[String] = NotAvailable.na,  `type`: Option[String] = NotAvailable.na,  parties: Parties, goodsItemCount: Option[String] = NotAvailable.na,  packageCount: Option[String] = NotAvailable.na)
case class Parties(partyIdentification: PartyIdentification)
case class PartyIdentification(number: Option[String] = NotAvailable.na)

object PartyIdentification { implicit val format: OFormat[PartyIdentification] = Json.format[PartyIdentification] }
object Parties { implicit val format: OFormat[Parties] = Json.format[Parties] }
object Declaration { implicit val format: OFormat[Declaration] = Json.format[Declaration] }
object DeclarationStatusResponse { implicit val format: OFormat[DeclarationStatusResponse] = Json.format[DeclarationStatusResponse] }
