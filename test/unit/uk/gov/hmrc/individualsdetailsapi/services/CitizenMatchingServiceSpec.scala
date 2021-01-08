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

package unit.uk.gov.hmrc.individualsdetailsapi.services

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsdetailsapi.domains.{
  MatchNotFoundException,
  MatchedCitizen
}
import uk.gov.hmrc.individualsdetailsapi.services.SandboxCitizenMatchingService
import unit.uk.gov.hmrc.individualsdetailsapi.utils.TestSupport
import uk.gov.hmrc.individualsdetailsapi.domains.SandboxDetailsData.{
  sandboxMatchId,
  sandboxNino
}

class CitizenMatchingServiceSpec
    extends TestSupport
    with MockitoSugar
    with ScalaFutures {

  implicit val hc = HeaderCarrier()
  val sandboxCitizenMatchingService = new SandboxCitizenMatchingService
  val matchedCitizen = MatchedCitizen(sandboxMatchId, sandboxNino)

  "sandboxCitizenMatching service match citizen function" should {
    "return a matchedCitizen for a valid matchId" in {
      await(sandboxCitizenMatchingService.matchCitizen(sandboxMatchId)(hc)) shouldBe matchedCitizen
    }

    "throw exception for an invalid matchId" in {
      intercept[MatchNotFoundException](
        await(
          sandboxCitizenMatchingService.matchCitizen(UUID.randomUUID())(hc)))
    }
  }
}
