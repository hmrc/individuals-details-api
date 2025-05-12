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

package unit.uk.gov.hmrc.individualsdetailsapi.domain.integrationframework

import play.api.libs.json.Json
import testUtils.TestHelpers
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.{IfContactDetail, IfDetails, IfDetailsResponse, IfResidence}
import unit.uk.gov.hmrc.individualsdetailsapi.utils.UnitSpec

class IfDetailResponseSpec extends UnitSpec with TestHelpers {
  val ninoDetails: IfDetails = IfDetails(Some("XH123456A"), None)
  val contactDetail1: IfContactDetail = IfContactDetail(9, "MOBILE TELEPHONE", "07123 987654")
  val contactDetail2: IfContactDetail = IfContactDetail(9, "MOBILE TELEPHONE", "07123 987655")
  val residence1: IfResidence =
    IfResidence(residenceType = Some("BASE"), address = generateAddress(2), noLongerUsed = Option("Y"))
  val residence2: IfResidence =
    IfResidence(residenceType = Some("NOMINATED"), address = generateAddress(1), noLongerUsed = Option("N"))
  val response: IfDetailsResponse = IfDetailsResponse(
    Some(Seq(contactDetail1, contactDetail1)),
    Some(Seq(residence1, residence2))
  )

  val invalidNinoDetails: IfDetails = IfDetails(Some("QWERTYUIOP"), None)
  val invalidContactDetail: IfContactDetail =
    IfContactDetail(-42, "abcdefghijklmnopqrstuvwxyz0123456789", "a")
  val invalidResidence: IfResidence =
    IfResidence(residenceType = Some(""), address = generateAddress(2))
  val invalidDetailsResponse: IfDetailsResponse = IfDetailsResponse(
    Some(Seq(invalidContactDetail)),
    Some(Seq(invalidResidence))
  )

  "Details Response" should {
    "Write to JSON" in {
      val result = Json.toJson(response)
      val expectedJson = Json.parse("""
                                      |  {
                                      |     "contactDetails" : [
                                      |       {
                                      |         "code" : 9,
                                      |         "type" : "MOBILE TELEPHONE",
                                      |         "detail" : "07123 987654"
                                      |       },
                                      |       {
                                      |         "code" : 9,
                                      |         "type" : "MOBILE TELEPHONE",
                                      |         "detail" : "07123 987654"
                                      |       }
                                      |     ],
                                      |     "residences" : [
                                      |       {
                                      |         "type" : "BASE",
                                      |         "address" : {
                                      |           "line1" : "line1-2",
                                      |           "line2" : "line2-2",
                                      |           "line3" : "line3-2",
                                      |           "line4" : "line4-2",
                                      |           "line5" : "line5-2",
                                      |           "postcode" : "QW122QW"
                                      |          },
                                      |          "noLongerUsed": "Y"
                                      |        },
                                      |        {
                                      |          "type" : "NOMINATED",
                                      |          "address" : {
                                      |            "line1" : "line1-1",
                                      |            "line2" : "line2-1",
                                      |            "line3" : "line3-1",
                                      |            "line4" : "line4-1",
                                      |            "line5" : "line5-1",
                                      |            "postcode" : "QW121QW"
                                      |          },
                                      |          "noLongerUsed": "N"
                                      |        } ]
                                      |      }""".stripMargin)

      result shouldBe expectedJson
    }

    "Validates successfully when reading valid Details Response" in {
      val result = Json.toJson(response).validate[IfDetailsResponse]
      result.isSuccess shouldBe true
    }

    "Validates unsuccessfully when reading invalid Details Response" in {
      val result =
        Json.toJson(invalidDetailsResponse).validate[IfDetailsResponse]
      result.isError shouldBe true
    }

  }
}
