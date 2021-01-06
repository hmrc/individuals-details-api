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

package uk.gov.hmrc.individualsdetailsapi.domains

import play.api.libs.functional.syntax.unlift
import play.api.libs.json.{Format, JsPath, Json}
import play.api.libs.functional.syntax._
import uk.gov.hmrc.individualsdetailsapi.domains.integrationframework.IfResidence

case class Residence(residenceType: Option[String],
                     address: Option[Address],
                     inUse: Option[Boolean])

case class Residences(residences: Seq[Residence])

object Residence {

  implicit val residenceFormat: Format[Residence] = Format(
    (
      (JsPath \ "residenceType").readNullable[String] and
        (JsPath \ "address").readNullable[Address] and
        (JsPath \ "inUse").readNullable[Boolean]
    )(Residence.apply _),
    (
      (JsPath \ "residenceType").writeNullable[String] and
        (JsPath \ "address").writeNullable[Address] and
        (JsPath \ "inUse").writeNullable[Boolean]
    )(unlift(Residence.unapply))
  )

  implicit val residencesFormat: Format[Residences] = Format(
    (JsPath \ "residence").read[Seq[Residence]].map(Residences.apply),
    (JsPath \ "residence").write[Seq[Residence]].contramap(x => x.residences)
  )

  def create(residenceType: Option[String],
             address: Option[Address],
             noLongerUsed: Option[Boolean]): Option[Residence] =
    (residenceType, address, noLongerUsed) match {
      case (None, None, None) => None
      case _                  => Some(new Residence(residenceType, address, noLongerUsed))
    }

  def create(residence: IfResidence): Option[Residence] = {

    val residenceType: Option[String] = residence.residenceType
    val address: Option[Address] = Address.create(residence.address)
    val inUse: Option[Boolean] = residence.noLongerUsed match {
      case Some("Y") => Some(false)
      case Some("N") => Some(true)
      case _         => None
    }

    create(residenceType, address, inUse)
  }
}
