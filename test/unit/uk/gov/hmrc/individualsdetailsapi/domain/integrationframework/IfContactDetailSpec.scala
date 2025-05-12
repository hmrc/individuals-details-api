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
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.IfContactDetail
import unit.uk.gov.hmrc.individualsdetailsapi.utils.UnitSpec

class IfContactDetailSpec extends UnitSpec {

  val contactDetail: IfContactDetail = IfContactDetail(9, "MOBILE TELEPHONE", "07123 987654")
  val invalidContactDetail: IfContactDetail =
    IfContactDetail(-42, "abcdefghijklmnopqrstuvwxyz0123456789", "a")

  "Contact details" should {
    "Write to JSON" in {
      val result = Json.toJson(contactDetail)
      val expectedJson = Json.parse("""
                                      |{
                                      |  "code" : 9,
                                      |  "type" : "MOBILE TELEPHONE",
                                      |  "detail" : "07123 987654"
                                      |}"""".stripMargin)

      result shouldBe expectedJson
    }

    "Validate successfully when reading valid contact details" in {
      val result = Json.toJson(contactDetail).validate[IfContactDetail]
      result.isSuccess shouldBe true
    }

    "Validate unsuccessfully when reading invalid contact details" in {
      val result = Json.toJson(invalidContactDetail).validate[IfContactDetail]
      result.isError shouldBe true
    }
  }
}
