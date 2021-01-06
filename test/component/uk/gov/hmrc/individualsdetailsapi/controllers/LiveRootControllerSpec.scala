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

import component.uk.gov.hmrc.individualsdetailsapi.stubs.{AuthStub, BaseSpec}
import play.api.test.Helpers._
import scalaj.http.Http

class LiveRootControllerSpec extends BaseSpec {

  val matchId: String = "2b2e7e84-102f-4338-93f9-1950b35d822b"

  val rootScope = "read:individuals-details"

  feature("Live Root Controller") {
    scenario("root route") {
      Given("A valid auth token ")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScope)

      When("I make a call to root endpoint")
      val response =
        Http(s"$serviceUrl/?matchId=$matchId")
          .headers(requestHeaders(acceptHeaderP1))
          .asString

      Then("The response tatus should be 500")
      response.code shouldBe INTERNAL_SERVER_ERROR

    }
  }

}
