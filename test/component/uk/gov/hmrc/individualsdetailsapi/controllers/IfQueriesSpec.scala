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

import component.uk.gov.hmrc.individualsdetailsapi.stubs.BaseSpec
import uk.gov.hmrc.individualsdetailsapi.services.ScopesHelper

class IfQueriesSpec extends BaseSpec {

  Feature("Query strings for addresses endpoint") {

    val endpoint = "addresses"
    val helper: ScopesHelper = app.injector.instanceOf[ScopesHelper]

    Scenario("For read:individuals-details-laa-c1") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-laa-c1"),
          List(endpoint))
      queryString shouldBe ""
    }

    Scenario("For read:individuals-details-laa-c2") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-laa-c2"),
          List(endpoint))
      queryString shouldBe ""
    }

    Scenario("For read:individuals-details-laa-c3") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-laa-c3"),
          List(endpoint))
      queryString shouldBe "residences(address(line1,line2,line3,line4,line5,postcode),noLongerUsed,type)&filter=contains(residences%5B%5D/noLongerUsed,'N')"
    }

    Scenario("For read:individuals-details-laa-c4") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-laa-c4"),
          List(endpoint))
      queryString shouldBe "residences(address(line1,line2,line3,line4,line5,postcode),noLongerUsed,type)&filter=contains(residences%5B%5D/noLongerUsed,'N')"
    }

    Scenario("For read:individuals-details-hmcts-c2") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-hmcts-c2"),
          List(endpoint))
      queryString shouldBe ""
    }

    Scenario("For read:individuals-details-hmcts-c3") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-hmcts-c3"),
          List(endpoint))
      queryString shouldBe "residences(address(line1,line2,line3,line4,line5,postcode),noLongerUsed,type)&filter=contains(residences%5B%5D/noLongerUsed,'N')"
    }

    Scenario("For read:individuals-details-hmcts-c4") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-hmcts-c4"),
          List(endpoint))
      queryString shouldBe "residences(address(line1,line2,line3,line4,line5,postcode),noLongerUsed,type)&filter=contains(residences%5B%5D/noLongerUsed,'N')"
    }

    Scenario("For read:individuals-details-lsani-c1") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-lsani-c1"),
          List(endpoint))
      queryString shouldBe "residences(address(line1,line2,line3,line4,line5,postcode),noLongerUsed,type)&filter=contains(residences%5B%5D/noLongerUsed,'N')"
    }

    Scenario("For read:individuals-details-lsani-c3") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-lsani-c3"),
          List(endpoint))
      queryString shouldBe "residences(address(line1,line2,line3,line4,line5,postcode),noLongerUsed,type)&filter=contains(residences%5B%5D/noLongerUsed,'N')"
    }

    Scenario("For read:individuals-details-nictsejo-c4") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-nictsejo-c4"),
          List(endpoint))
      queryString shouldBe "residences(address(line1,line2,line3,line4,line5,postcode),noLongerUsed,type)&filter=contains(residences%5B%5D/noLongerUsed,'N')"
    }
  }

  Feature("Query strings for contact-details endpoint") {

    val helper: ScopesHelper = app.injector.instanceOf[ScopesHelper]
    val endpoint = "contact-details"

    Scenario("For read:individuals-details-laa-c1") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-laa-c1"),
          List(endpoint))
      queryString shouldBe ""
    }

    Scenario("For read:individuals-details-laa-c2") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-laa-c2"),
          List(endpoint))
      queryString shouldBe ""
    }

    Scenario("For read:individuals-details-laa-c3") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-laa-c3"),
          List(endpoint))
      queryString shouldBe ""
    }

    Scenario("For read:individuals-details-laa-c4") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-laa-c4"),
          List(endpoint))
      queryString shouldBe "contactDetails(code,detail,type)&filter=contains(contactDetails%5B%5D/type,'TELEPHONE')"
    }

    Scenario("For read:individuals-details-hmcts-c2") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-hmcts-c2"),
          List(endpoint))
      queryString shouldBe ""
    }

    Scenario("For read:individuals-details-hmcts-c3") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-hmcts-c3"),
          List(endpoint))
      queryString shouldBe ""
    }

    Scenario("For read:individuals-details-hmcts-c4") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-hmcts-c4"),
          List(endpoint))
      queryString shouldBe "contactDetails(code,detail,type)&filter=contains(contactDetails%5B%5D/type,'TELEPHONE')"
    }

    Scenario("For read:individuals-details-lsani-c1") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-lsani-c1"),
          List(endpoint))
      queryString shouldBe ""
    }

    Scenario("For read:individuals-details-lsani-c3") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-lsani-c3"),
          List(endpoint))
      queryString shouldBe ""
    }

    Scenario("For read:individuals-details-nictsejo-c4") {
      val queryString =
        helper.getQueryStringFor(Seq("read:individuals-details-nicts-c4"),
          List(endpoint))
      queryString shouldBe ""
    }

  }
}
