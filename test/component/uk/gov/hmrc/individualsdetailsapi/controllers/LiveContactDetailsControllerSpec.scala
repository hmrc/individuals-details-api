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

package component.uk.gov.hmrc.individualsdetailsapi.controllers

import play.api.libs.json.{JsValue, Json}

import java.util.UUID

class LiveContactDetailsControllerSpec extends CommonControllerWithIfRequestSpec {

  override val matchId: UUID =
    UUID.fromString("2b2e7e84-102f-4338-93f9-1950b35d822b")
  override val endpoint: String = "contact-details"
  override val nino = "AB123456C"
  override val rootScope =
    List("read:individuals-details-hmcts-c4", "read:individuals-details-laa-c4")

  override val expectedJson: JsValue =
    Json.parse(s"""{
                  |  "_links" : {
                  |    "self" : {
                  |      "href" : "/individuals/details/contact-details?matchId=$matchId"
                  |    }
                  |  },
                  |  "contactDetails" : {
                  |    "daytimeTelephones" : [ "01234 567890" ],
                  |    "eveningTelephones" : [ "01234 567890" ],
                  |    "mobileTelephones" : [ "01234 567890" ]
                  |  }
                  |}""".stripMargin)

}
