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

package component.uk.gov.hmrc.individualsdetailsapi.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.IfDetailsResponse

object IfStub extends MockHost(22004) {

  def searchDetails(nino: String, ifDetailsResponse: IfDetailsResponse) =
    mock.register(
      get(urlPathEqualTo(s"/individuals/details/contact/nino/$nino"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(Json.toJson(ifDetailsResponse).toString())
        )
    )

  def customResponse(nino: String, status: Int, response: JsValue) =
    mock.register(
      get(urlPathEqualTo(s"/individuals/details/contact/nino/$nino"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response.toString())
        )
    )

  def enforceRateLimit(nino: String, fromDate: String, toDate: String): Unit =
    mock.register(
      get(urlPathEqualTo(s"/individuals/details/contact"))
        .withQueryParam(
          "fields",
          equalTo(
            "employments(employer(address(line1,line2,line3,line4,line5,postcode),districtNumber,name,schemeRef),employment(endDate,startDate))"
          )
        )
        .willReturn(aResponse().withStatus(TOO_MANY_REQUESTS))
    )

}
