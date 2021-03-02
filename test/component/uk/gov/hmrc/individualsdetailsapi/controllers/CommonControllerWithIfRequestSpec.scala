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

import component.uk.gov.hmrc.individualsdetailsapi.stubs.{AuthStub, IfStub, IndividualsMatchingApiStub}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.individualsdetailsapi.domain.SandboxDetailsData
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.{IfAddress, IfContactDetail, IfDetailsResponse, IfResidence}

trait CommonControllerWithIfRequestSpec extends CommonControllerSpec {

  val invalidIfDetailsResponse: IfDetailsResponse = SandboxDetailsData
    .findByMatchId(SandboxDetailsData.sandboxMatchId).map(_.copy(
    contactDetails = Option(Seq(
      IfContactDetail(
        code = 7,
        detailType = "DAYTIME TELEPHONE",
        detail = ""
      ))),
    residences = Option(Seq(
      IfResidence(
        address = Option(IfAddress(
          line1 = Option(""),
          line2 = Option("Dawley Bank"),
          line3 = Option("Telford"),
          line4 = Option("Shropshire"),
          postcode = Option("TF3 4ER")
        )),
        noLongerUsed = Option("N")
      )
    ))
  )).map(data => IfDetailsResponse(
    data.contactDetails,
    data.residences
  )).get

  scenario(s"user does not have valid scopes") {
    Given("A valid auth token but invalid scopes")
    AuthStub.willNotAuthorizePrivilegedAuthTokenNoScopes(authToken)

    And("a valid record in the matching API")
    IndividualsMatchingApiStub.hasMatchFor(matchId.toString, nino)

    And("IF will return response")
    IfStub.searchDetails(nino, ifDetailsResponse)

    When(
      s"I make a call to ${if (endpoint.isEmpty) "root" else endpoint} endpoint")
    val response = invokeEndpoint(s"$serviceUrl/${endpoint}?matchId=$matchId")

    Then("The response status should be 401")
    response.code shouldBe UNAUTHORIZED
    Json.parse(response.body) shouldBe Json.obj(
      "code" -> "UNAUTHORIZED",
      "message" ->"Insufficient Enrolments"
    )
  }

  scenario(s"valid request but invalid IF response") {

    Given("A valid auth token ")
    AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScope)

    And("a valid record in the matching API")
    IndividualsMatchingApiStub.hasMatchFor(matchId.toString, nino)

    And("IF will return invalid response")
    IfStub.searchDetails(nino, invalidIfDetailsResponse)

    When(
      s"I make a call to ${if (endpoint.isEmpty) "root" else endpoint} endpoint")
    val response = invokeEndpoint(s"$serviceUrl/${endpoint}?matchId=$matchId")

    Then("The response status should be 500 with a generic error message")
    response.code shouldBe INTERNAL_SERVER_ERROR
    Json.parse(response.body) shouldBe Json.obj(
      "code" -> "INTERNAL_SERVER_ERROR",
      "message" -> "Something went wrong.")
  }

  scenario(s"IF returns an Internal Server Error") {

    Given("A valid auth token ")
    AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScope)

    And("a valid record in the matching API")
    IndividualsMatchingApiStub.hasMatchFor(matchId.toString, nino)

    And("IF will return Internal Server Error")
    IfStub.customResponse(nino, INTERNAL_SERVER_ERROR, Json.obj("reason" -> "Server error"))

    When(
      s"I make a call to ${if (endpoint.isEmpty) "root" else endpoint} endpoint")
    val response = invokeEndpoint(s"$serviceUrl/${endpoint}?matchId=$matchId")

    Then("The response status should be 500 with a generic error message")
    response.code shouldBe INTERNAL_SERVER_ERROR
    Json.parse(response.body) shouldBe Json.obj(
      "code" -> "INTERNAL_SERVER_ERROR",
      "message" -> "Something went wrong.")
  }

  scenario(s"IF returns an Bad Request Error") {

    Given("A valid auth token ")
    AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScope)

    And("a valid record in the matching API")
    IndividualsMatchingApiStub.hasMatchFor(matchId.toString, nino)

    And("IF will return Internal Server Error")
    IfStub.customResponse(nino, UNPROCESSABLE_ENTITY, Json.obj("reason" ->
      "There are 1 or more unknown data items in the 'fields' query string"))

    When(
      s"I make a call to ${if (endpoint.isEmpty) "root" else endpoint} endpoint")
    val response = invokeEndpoint(s"$serviceUrl/${endpoint}?matchId=$matchId")

    Then("The response status should be 500 with a generic error message")
    response.code shouldBe INTERNAL_SERVER_ERROR
    Json.parse(response.body) shouldBe Json.obj(
      "code" -> "INTERNAL_SERVER_ERROR",
      "message" -> "Something went wrong.")
  }
}
