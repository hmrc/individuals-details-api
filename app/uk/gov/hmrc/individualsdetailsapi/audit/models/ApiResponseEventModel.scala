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

package uk.gov.hmrc.individualsdetailsapi.audit.models

import play.api.libs.json.Json
import uk.gov.hmrc.individualsdetailsapi.domain.{ContactDetails, Residence}

case class ContactDetailsApiResponseEventModel(ipAddress: String,
                                 authorisation: String,
                                 deviceId: String,
                                 input: String,
                                 method: String,
                                 userAgent: String,
                                 apiVersion: String,
                                 matchId: String,
                                 correlationId: Option[String],
                                 scopes: String,
                                 returnLinks: String,
                                 data: Option[ContactDetails])

object ContactDetailsApiResponseEventModel {
  implicit val formatApiResponseEventModel = Json.format[ContactDetailsApiResponseEventModel]
}

case class ResidencesApiResponseEventModel(ipAddress: String,
                                 authorisation: String,
                                 deviceId: String,
                                 input: String,
                                 method: String,
                                 userAgent: String,
                                 apiVersion: String,
                                 matchId: String,
                                 correlationId: Option[String],
                                 scopes: String,
                                 returnLinks: String,
                                 data: Seq[Residence])

object ResidencesApiResponseEventModel {
  implicit val formatApiResponseEventModel = Json.format[ResidencesApiResponseEventModel]
}

case class ApiResponseEventModel(ipAddress: String,
                                           authorisation: String,
                                           deviceId: String,
                                           input: String,
                                           method: String,
                                           userAgent: String,
                                           apiVersion: String,
                                           matchId: String,
                                           correlationId: Option[String],
                                           scopes: String,
                                           returnLinks: String)

object ApiResponseEventModel {
  implicit val formatApiResponseEventModel = Json.format[ApiResponseEventModel]
}
