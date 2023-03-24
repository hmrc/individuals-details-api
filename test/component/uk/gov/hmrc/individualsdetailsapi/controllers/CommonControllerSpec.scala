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

import java.util.UUID

import component.uk.gov.hmrc.individualsdetailsapi.stubs.{AuthStub, BaseSpec, IfStub, IndividualsMatchingApiStub}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.individualsdetailsapi.domain.SandboxDetailsData
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.IfDetailsResponse

trait CommonControllerSpec extends BaseSpec {

  val rootScope: List[String]
  val endpoint: String
  val matchId: UUID
  val nino: String
  val expectedJson: JsValue
  val ifDetailsResponse: IfDetailsResponse = SandboxDetailsData
    .findByMatchId(SandboxDetailsData.sandboxMatchId)
    .map(
      sandboxData =>
        IfDetailsResponse(
          sandboxData.contactDetails,
          sandboxData.residences
      )
    )
    .get

  Feature(s"Common controller") {

    Scenario("missing match id") {

      When("the root entry point to the API is invoked with a missing match id")
      val response = invokeEndpoint(s"$serviceUrl/$endpoint")

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code" -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )
    }

    Scenario("malformed match id") {

      When(
        "the root entry point to the API is invoked with a malformed match id")
      val response =
        invokeEndpoint(
          s"$serviceUrl/${endpoint}?matchId=malformed-match-id-value")

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code" -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )
    }

    Scenario("invalid match id") {

      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScope)

      When(
        "the root entry point to the API is invoked with an invalid match id")
      val response = invokeEndpoint(
        s"$serviceUrl/${endpoint}?matchId=0a184ef3-fd75-4d4d-b6a3-f886cc39a366")

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code" -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )
    }

    Scenario(s"valid request and response") {
      Given("A valid auth token ")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScope)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.hasMatchFor(matchId.toString, nino)

      And("IF will return response")
      IfStub.searchDetails(nino, ifDetailsResponse)

      When(
        s"I make a call to ${if (endpoint.isEmpty) "root" else endpoint} endpoint")
      val response = invokeEndpoint(s"$serviceUrl/${endpoint}?matchId=$matchId")

      Then("The response status should be 200")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe expectedJson
    }
  }
}
