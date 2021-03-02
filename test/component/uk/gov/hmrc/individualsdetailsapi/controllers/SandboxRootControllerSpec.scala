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

package component.uk.gov.hmrc.individualsdetailsapi.controllers

import java.util.UUID

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.individualsdetailsapi.domain.SandboxDetailsData

class SandboxRootControllerSpec extends CommonControllerSpec {

  override val matchId: UUID = SandboxDetailsData.sandboxMatchId
  override val endpoint: String = "sandbox"
  override val nino = "AB123456C"

  override val rootScope = List(
    "read:individuals-details-hmcts-c3",
    "read:individuals-details-hmcts-c4",
    "read:individuals-details-laa-c3",
    "read:individuals-details-laa-c4",
    "read:individuals-details-lsani-c1",
    "read:individuals-details-lsani-c3",
    "read:individuals-details-nictsejo-c4"
  )

  override val expectedJson: JsValue = Json.parse(s"""{
   |  "_links" : {
   |    "addresses" : {
   |      "href" : "/individuals/details/addresses?matchId=$matchId",
   |      "title" : "Get addresses"
   |    },
   |    "contact-details" : {
   |      "href" : "/individuals/details/contact-details?matchId=$matchId",
   |      "title" : "Get contact details"
   |    },
   |    "self" : {
   |      "href" : "/individuals/details/?matchId=$matchId"
   |    }
   |  }
   |}""".stripMargin)

}
