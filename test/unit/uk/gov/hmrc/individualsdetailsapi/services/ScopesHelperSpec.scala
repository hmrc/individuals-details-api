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

package unit.uk.gov.hmrc.individualsdetailsapi.services

import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.individualsdetailsapi.services.{ScopesHelper, ScopesService}
import unit.uk.gov.hmrc.individualsdetailsapi.utils.UnitSpec

class ScopesHelperSpec extends UnitSpec with ScopesConfig with BeforeAndAfterEach {

  "Scopes helper" should {

    val scopesService = new ScopesService(mockConfig)
    val scopesHelper = new ScopesHelper(scopesService)

    "return correct query string" in {
      val scopeOneResult =
        scopesHelper.getQueryStringFor(List(mockScopeOne), List(endpointOne, endpointTwo, endpointThree))
      scopeOneResult shouldBe "path(to(a,b,c,d))"

      val scopeOneEndpointOneResult =
        scopesHelper.getQueryStringFor(List(mockScopeOne), List(endpointOne))
      scopeOneEndpointOneResult shouldBe "path(to(a,b,c))"

      val scopeOneEndpointTwoResult =
        scopesHelper.getQueryStringFor(List(mockScopeOne), List(endpointTwo))
      scopeOneEndpointTwoResult shouldBe "path(to(d))"

      val scopeOneEndpointThreeResult =
        scopesHelper.getQueryStringFor(List(mockScopeOne), List(endpointThree))
      scopeOneEndpointThreeResult shouldBe ""

      val scopeTwoResult =
        scopesHelper.getQueryStringFor(List(mockScopeTwo), List(endpointOne, endpointTwo, endpointThree))
      scopeTwoResult shouldBe "path(to(e,f,g,h,i))"

      val twoScopesResult =
        scopesHelper.getQueryStringFor(List(mockScopeOne, mockScopeTwo), List(endpointOne, endpointTwo, endpointThree))
      twoScopesResult shouldBe "path(to(a,b,c,d,e,f,g,h,i))"
    }

    "return correct query string with filter" in {
      val scopeThreeResult =
        scopesHelper.getQueryStringFor(List(mockScopeThree), List(endpointThree))
      scopeThreeResult shouldBe "path(to(g,h,i))&filter=contains(path/to/g,'FILTERED_VALUE_1')"

      val scopeFourFirstResult =
        scopesHelper.getQueryStringFor(List(mockScopeFour), List(endpointThree))
      scopeFourFirstResult shouldBe "path(to(g,h,i))&filter=contains(path/to/g,'FILTERED_VALUE_2')"

      val scopeFourSecondResult =
        scopesHelper.getQueryStringFor(List(mockScopeFour), List(endpointFour))
      scopeFourSecondResult shouldBe "path(to(j))&filter=contains(path/to/j,'<token>')"
    }
  }
}
