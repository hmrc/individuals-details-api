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
import uk.gov.hmrc.individualsdetailsapi.domains.integrationframework.IFDetails
import unit.uk.gov.hmrc.individualsdetailsapi.utils.UnitSpec

class IFDetailsSpec extends UnitSpec {

  val ninoDetails = IFDetails(Some("XH123456A"), None)
  val trnDetails = IFDetails(None, Some("12345678"))

  val invalidNinoDetails = IFDetails(Some("QWERTYUIOP"), None)
  val invalidTrnDetails = IFDetails(None, Some("QWERTYUIOP"))

  "Details" should {
    "Write to JSON when only nino provided" in {
      val result = Json.toJson(ninoDetails)
      val expectedJson = Json.parse("""
          |{
          |   "nino" : "XH123456A"
          |}
        """.stripMargin)
      result shouldBe expectedJson
    }

    "Write to JSON when only trn provided" in {
      val result = Json.toJson(trnDetails)
      val expectedJson = Json.parse("""
          |{
          |  "trn" : "12345678"
          |}
        """.stripMargin)

      result shouldBe expectedJson
    }

    "Validate successful when reading valid nino" in {
      val result = Json.toJson(ninoDetails).validate[IFDetails]
      result.isSuccess shouldBe true
    }

    "Validate successful when reading valid trn" in {
      val result = Json.toJson(trnDetails).validate[IFDetails]
      result.isSuccess shouldBe true
    }

    "Validate unsuccessfully when reading invalid nino" in {
      val result = Json.toJson(invalidNinoDetails).validate[IFDetails]
      result.isError shouldBe true
    }

    "Validate unsuccessfully when reading invalid trn" in {
      val result = Json.toJson(invalidTrnDetails).validate[IFDetails]
      result.isError shouldBe true
    }
  }
}
