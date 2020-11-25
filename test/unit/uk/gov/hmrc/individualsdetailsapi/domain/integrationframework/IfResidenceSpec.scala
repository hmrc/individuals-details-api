/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.individualsdetailsapi.domains.integrationframework.IfResidence
import unit.uk.gov.hmrc.individualsdetailsapi.utils.UnitSpec

class IfResidenceSpec extends UnitSpec with TestHelpers {

  val residence =
    IfResidence(residenceType = Some("BASE"), address = generateAddress(2))
  val invalidResidence =
    IfResidence(residenceType = Some(""), address = generateAddress(2))

  "Residence details" should {
    "Write to JSON" in {
      val result = Json.toJson(residence)
      val expectedJson = Json.parse("""
          |{
          |  "type" : "BASE",
          |  "address" : {
          |      "line1" : "line1-2",
          |      "line2" : "line2-2",
          |      "line3" : "line3-2",
          |      "line4" : "line4-2",
          |      "line5" : "line5-2",
          |      "postcode" : "QW122QW"
          |  }
          |}
        """.stripMargin)

      result shouldBe expectedJson
    }

    "Validate successfully when reading valid Residence information" in {
      val result = Json.toJson(residence).validate[IfResidence]
      result.isSuccess shouldBe true
    }

    "Validate unsuccessfully when reading invalid Residence information" in {
      val result = Json.toJson(invalidResidence).validate[IfResidence]
      result.isError shouldBe true
    }

  }
}
