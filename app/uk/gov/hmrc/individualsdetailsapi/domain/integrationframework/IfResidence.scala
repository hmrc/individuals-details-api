/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.individualsdetailsapi.domain.integrationframework

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.{max, maxLength, min, minLength, pattern}
import play.api.libs.json.{Format, JsPath}

import scala.util.matching.Regex

case class IfResidence(
  statusCode: Option[String] = None,
  status: Option[String] = None,
  typeCode: Option[Int] = None,
  residenceType: Option[String] = None,
  deliveryInfo: Option[String] = None,
  retLetterServ: Option[String] = None,
  addressCode: Option[String] = None,
  addressType: Option[String] = None,
  address: Option[IfAddress] = None,
  houseId: Option[String] = None,
  homeCountry: Option[String] = None,
  otherCountry: Option[String] = None,
  deadLetterOfficeDate: Option[String] = None,
  startDateTime: Option[String] = None,
  noLongerUsed: Option[String] = None
)

object IfResidence {

  private val statusCodePattern: Regex = "^[1-9]$".r
  private val datePattern: Regex =
    """^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)
      |[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|
      |(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$""".r

  implicit val residencesFormat: Format[IfResidence] = Format(
    (
      (JsPath \ "statusCode").readNullable[String](pattern(statusCodePattern, "Status code is invalid")) and
        (JsPath \ "status").readNullable[String](minLength[String](1) andKeep maxLength[String](8)) and
        (JsPath \ "typeCode").readNullable[Int](min[Int](1) andKeep max[Int](9999)) and
        (JsPath \ "type").readNullable[String](minLength[String](1) andKeep maxLength[String](35)) and
        (JsPath \ "deliveryInfo").readNullable[String](minLength[String](1) andKeep maxLength[String](35)) and
        (JsPath \ "retLetterServ").readNullable[String](minLength[String](1) andKeep maxLength[String](1)) and
        (JsPath \ "addressCode").readNullable[String](pattern(statusCodePattern, "Address code is invalid")) and
        (JsPath \ "addressType").readNullable[String](minLength[String](1) andKeep maxLength[String](6)) and
        (JsPath \ "address").readNullable[IfAddress] and
        (JsPath \ "houseId").readNullable[String](maxLength[String](35)) and
        (JsPath \ "homeCountry").readNullable[String](maxLength[String](16)) and
        (JsPath \ "otherCountry").readNullable[String](maxLength[String](35)) and
        (JsPath \ "deadLetterOfficeDate").readNullable[String](pattern(datePattern, "Date is invalid")) and
        (JsPath \ "startDateTime").readNullable[String] and
        (JsPath \ "noLongerUsed").readNullable[String](minLength[String](1) andKeep maxLength[String](1))
    )(IfResidence.apply _),
    (
      (JsPath \ "statusCode").writeNullable[String] and
        (JsPath \ "status").writeNullable[String] and
        (JsPath \ "typeCode").writeNullable[Int] and
        (JsPath \ "type").writeNullable[String] and
        (JsPath \ "deliveryInfo").writeNullable[String] and
        (JsPath \ "retLetterServ").writeNullable[String] and
        (JsPath \ "addressCode").writeNullable[String] and
        (JsPath \ "addressType").writeNullable[String] and
        (JsPath \ "address").writeNullable[IfAddress] and
        (JsPath \ "houseId").writeNullable[String] and
        (JsPath \ "homeCountry").writeNullable[String] and
        (JsPath \ "otherCountry").writeNullable[String] and
        (JsPath \ "deadLetterOfficeDate").writeNullable[String] and
        (JsPath \ "startDateTime").writeNullable[String] and
        (JsPath \ "noLongerUsed").writeNullable[String]
    )(o => Tuple.fromProductTyped(o))
  )
}
