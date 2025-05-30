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

import play.api.libs.json.{JsResult, Json}
import testUtils.TestHelpers
import uk.gov.hmrc.individualsdetailsapi.domain.Address
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework._
import unit.uk.gov.hmrc.individualsdetailsapi.utils.UnitSpec

class IfAddressSpec extends UnitSpec with TestHelpers {

  val address: IfAddress = IfAddress(
    Some("line1"),
    Some("line2"),
    Some("line3"),
    Some("line4"),
    Some("line5"),
    Some("postcode")
  )
  "Address" should {

    "write to JSON successfully" in {
      val result = Json.toJson(address).validate[IfAddress]
      result.isSuccess shouldBe true
    }

    "convert to Address successfully when address is empty" in {
      val result: JsResult[IfAddress] = Json
        .toJson(IfAddress(None, None, None, None, None, None))
        .validate[IfAddress]
      result.isSuccess shouldBe true
      val convertedAddress: Option[Address] = Address.convert(Some(result.get))
      convertedAddress.isEmpty shouldBe true
    }

    "convert to Address successfully when address has data" in {
      val result = Json.toJson(address).validate[IfAddress]
      result.isSuccess shouldBe true
      val convertedAddress: Option[Address] = Address.convert(Some(result.get))
      convertedAddress.isDefined shouldBe true
    }

    "validate successfully" when {
      "lines are equal to min length" in {
        val line = generateString(1)
        val result =
          Json.toJson(address.copy(line1 = Some(line))).validate[IfAddress]
        result.isSuccess shouldBe true
      }

      "lines are equal to max length" in {
        val line = generateString(35)
        val result =
          Json.toJson(address.copy(line1 = Some(line))).validate[IfAddress]
        result.isSuccess shouldBe true
      }
    }

    "fail to validate" when {
      "lines are longer than max length" in {
        val line = generateString(101)
        val result =
          Json.toJson(address.copy(line1 = Some(line))).validate[IfAddress]
        result.isError shouldBe true
      }
    }
  }
}
