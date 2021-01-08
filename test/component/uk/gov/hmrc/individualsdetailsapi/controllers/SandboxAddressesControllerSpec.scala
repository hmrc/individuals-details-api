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

class SandboxAddressesControllerSpec extends CommonControllerSpec {

  override val rootScope =
    List("read:individuals-details-hmcts-c4", "read:individuals-details-laa-c4")

  override val matchId: UUID = SandboxDetailsData.sandboxMatchId
  override val endpoint: String = "sandbox/addresses"
  override val nino = "AB123456C"

  override val expectedJson: JsValue = Json.parse(s"""{
   |  "_links" : {
   |    "self" : {
   |      "href" : "/individuals/details/addresses?matchId=$matchId"
   |    }
   |  },
   |  "residences" : [ {
   |    "residenceType" : "NOMINATED",
   |    "address" : {
   |      "line1" : "24 Trinity Street",
   |      "line2" : "Dawley Bank",
   |      "line3" : "Telford",
   |      "line4" : "Shropshire",
   |      "line5" : "UK",
   |      "postcode" : "TF3 4ER"
   |    },
   |    "inUse" : true
   |  }, {
   |    "residenceType" : "BASE",
   |    "address" : {
   |      "line1" : "La Petite Maison",
   |      "line2" : "Rue de Bastille",
   |      "line3" : "Vieux Ville",
   |      "line4" : "Dordogne",
   |      "line5" : "France"
   |    },
   |    "inUse" : false
   |  } ]
   |}""".stripMargin)
}
