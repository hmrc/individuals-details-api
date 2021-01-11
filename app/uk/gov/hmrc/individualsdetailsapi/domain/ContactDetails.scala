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

package uk.gov.hmrc.individualsdetailsapi.domain

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.IfContactDetail

case class ContactDetails(daytimeTelephone: List[String],
                          eveningTelephone: List[String],
                          mobileTelephone: List[String])

object ContactDetails {

  implicit val format: Format[ContactDetails] = Json.format[ContactDetails]

  private val DaytimeTelephone: Int = 7
  private val EveningTelephone: Int = 8
  private val MobileTelephone: Int = 9

  def create(daytimeTelephone: List[String],
             eveningTelephone: List[String],
             mobileTelephone: List[String]): Option[ContactDetails] =
    (daytimeTelephone, eveningTelephone, mobileTelephone) match {
      case (a, b, c) if a.isEmpty && b.isEmpty && c.isEmpty => None
      case _ =>
        Option(
          new ContactDetails(daytimeTelephone,
                             eveningTelephone,
                             mobileTelephone))
    }

  def create(contactDetails: Seq[IfContactDetail]): Option[ContactDetails] =
    contactDetails.foldRight(
      (List.empty[String], List.empty[String], List.empty[String]))(
      (detail, tuple) =>
        detail.code match {
          case DaytimeTelephone =>
            tuple.copy(_1 = tuple._1.++(Seq(detail.detail)))
          case EveningTelephone =>
            tuple.copy(_2 = tuple._2.++(Seq(detail.detail)))
          case MobileTelephone =>
            tuple.copy(_3 = tuple._3.++(Seq(detail.detail)))
          case _ => tuple
      }) match {
      case (daytimeTelephone: List[String],
            eveningTelephone: List[String],
            mobileTelephone: List[String]) =>
        create(daytimeTelephone, eveningTelephone, mobileTelephone)
    }
}
