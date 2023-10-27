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

package testUtils

import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.{IfAddress, IfContactDetail, IfDetailsResponse, IfResidence}

import scala.util.Random

trait TestHelpers {

  def generateString(length: Int): String = {
    val chars = "abcdefghijklmnopqrstuvwxyz123456789"

    def generate(string: String): String =
      if (string.length < length)
        generate(string.concat(chars.charAt(Random.nextInt(chars.length - 1)).toString))
      else
        string

    generate("")
  }

  def generateAddress(number: Int) =
    Some(
      IfAddress(
        Some(s"line1-$number"),
        Some(s"line2-$number"),
        Some(s"line3-$number"),
        Some(s"line4-$number"),
        Some(s"line5-$number"),
        Some(s"QW12${number}QW"),
      ))

  def createEmptyDetailsResponse(): IfDetailsResponse =
    IfDetailsResponse(None, None)

  def createValidIFDetailsResponse(): IfDetailsResponse = {
    val contactDetail1 = IfContactDetail(9, "MOBILE TELEPHONE", "07123 987654")
    val contactDetail2 = IfContactDetail(9, "MOBILE TELEPHONE", "07123 987655")
    val residence1 =
      IfResidence(residenceType = Some("BASE"), address = generateAddress(2))
    val residence2 = IfResidence(residenceType = Some("NOMINATED"), address = generateAddress(1))

    IfDetailsResponse(
      Some(Seq(contactDetail1, contactDetail2)),
      Some(Seq(residence1, residence2))
    )
  }
}
