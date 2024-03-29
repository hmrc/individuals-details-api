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

package uk.gov.hmrc.individualsdetailsapi.audit.models

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.individualsdetailsapi.domain.{ContactDetails, Residence}

case class ContactDetailsApiResponseEventModel(
  deviceId: String,
  input: String,
  method: String,
  userAgent: String,
  apiVersion: String,
  matchId: String,
  correlationId: Option[String],
  applicationId: String,
  scopes: String,
  returnLinks: String,
  contactDetails: Option[ContactDetails]
)

object ContactDetailsApiResponseEventModel {
  implicit val formatApiResponseEventModel: OFormat[ContactDetailsApiResponseEventModel] =
    Json.format[ContactDetailsApiResponseEventModel]
}

case class ResidencesApiResponseEventModel(
  deviceId: String,
  input: String,
  method: String,
  userAgent: String,
  apiVersion: String,
  matchId: String,
  correlationId: Option[String],
  applicationId: String,
  scopes: String,
  returnLinks: String,
  residences: Seq[Residence]
)

object ResidencesApiResponseEventModel {
  implicit val formatApiResponseEventModel: OFormat[ResidencesApiResponseEventModel] =
    Json.format[ResidencesApiResponseEventModel]
}

case class ApiResponseEventModel(
  deviceId: String,
  input: String,
  method: String,
  userAgent: String,
  apiVersion: String,
  matchId: String,
  correlationId: Option[String],
  applicationId: String,
  scopes: String,
  returnLinks: String
)

object ApiResponseEventModel {
  implicit val formatApiResponseEventModel: OFormat[ApiResponseEventModel] = Json.format[ApiResponseEventModel]
}
