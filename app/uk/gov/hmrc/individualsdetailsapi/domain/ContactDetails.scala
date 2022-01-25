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

package uk.gov.hmrc.individualsdetailsapi.domain

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.IfContactDetail

case class ContactDetails(daytimeTelephones: List[String],
                          eveningTelephones: List[String],
                          mobileTelephones: List[String])

object ContactDetails {

  implicit val format: Format[ContactDetails] = Json.format[ContactDetails]

  private val DaytimeTelephone: Int = 7
  private val EveningTelephone: Int = 8
  private val MobileTelephone: Int = 9

  def create(daytimeTelephones: List[String],
             eveningTelephones: List[String],
             mobileTelephones: List[String]): Option[ContactDetails] =
    (daytimeTelephones, eveningTelephones, mobileTelephones) match {
      case (a, b, c) if a.isEmpty && b.isEmpty && c.isEmpty => None
      case _ =>
        Option(
          new ContactDetails(daytimeTelephones,
                             eveningTelephones,
                             mobileTelephones))
    }

  def convert(contactDetails: Seq[IfContactDetail]): Option[ContactDetails] =
    (create _).tupled apply contactDetails.foldRight(
      (List.empty[String], List.empty[String], List.empty[String]))(
      (detail, tuple) =>
        detail.code match {
          case DaytimeTelephone => tuple.copy(_1 = tuple._1.+:(detail.detail))
          case EveningTelephone => tuple.copy(_2 = tuple._2.+:(detail.detail))
          case MobileTelephone  => tuple.copy(_3 = tuple._3.+:(detail.detail))
          case _                => tuple
      })
}
