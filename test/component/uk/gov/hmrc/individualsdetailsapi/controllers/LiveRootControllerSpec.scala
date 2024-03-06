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

import component.uk.gov.hmrc.individualsdetailsapi.stubs.{AuthStub, IfStub, IndividualsMatchingApiStub}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._

import java.util.UUID

class LiveRootControllerSpec extends CommonControllerSpec {

  override val matchId: UUID =
    UUID.fromString("2b2e7e84-102f-4338-93f9-1950b35d822b")
  override val endpoint: String = ""
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

  override val expectedJson: JsValue =
    Json.parse(s"""{
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

  Scenario(s"user does not have valid scopes") {
    Given("A valid auth token but invalid scopes")
    AuthStub.willNotAuthorizePrivilegedAuthTokenNoScopes(authToken)

    And("a valid record in the matching API")
    IndividualsMatchingApiStub.hasMatchFor(matchId.toString, nino)

    And("IF will return response")
    IfStub.searchDetails(nino, ifDetailsResponse)

    When(s"I make a call to ${if (endpoint.isEmpty) "root" else endpoint} endpoint")
    val response = invokeEndpoint(s"$serviceUrl/$endpoint?matchId=$matchId")

    Then("The response status should be 401")
    response.code shouldBe UNAUTHORIZED
    Json.parse(response.body) shouldBe Json.obj(
      "code"    -> "UNAUTHORIZED",
      "message" -> "Insufficient Enrolments"
    )
  }
}
