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

package uk.gov.hmrc.individualsdetailsapi.domain

import uk.gov.hmrc.domain.{EmpRef, Nino, SaUtr}
import uk.gov.hmrc.individualsdetailsapi.domain.integrationframework.{IfAddress, IfContactDetail, IfResidence}

import java.util.UUID

case class MatchedCitizen(matchId: UUID, nino: Nino)

case class Individual(
  matchId: UUID,
  nino: String,
  contactDetails: Option[Seq[IfContactDetail]],
  residences: Option[Seq[IfResidence]]
)

object SandboxDetailsData {

  def findByMatchId(matchId: UUID) = individuals.find(_.matchId == matchId)

  def matchedCitizen(matchId: UUID) = matchId match {
    case `sandboxMatchId` => Some(MatchedCitizen(sandboxMatchId, sandboxNino))
    case _                => None
  }

  private lazy val individuals = Seq(amanda())

  val sandboxNino = Nino("NA000799C")

  val sandboxMatchId = UUID.fromString("57072660-1df9-4aeb-b4ea-cd2d7f96e430")

  val acmeEmployerReference = EmpRef.fromIdentifiers("123/AI45678")

  val disneyEmployerReference = EmpRef.fromIdentifiers("123/DI45678")

  val sandboxUtr = SaUtr("2432552635")

  private def amanda() = Individual(
    matchId = sandboxMatchId,
    nino = sandboxNino.nino,
    contactDetails = Some(
      Seq(
        IfContactDetail(
          code = 7,
          detailType = "DAYTIME TELEPHONE",
          detail = "01234 567890"
        ),
        IfContactDetail(
          code = 8,
          detailType = "EVENING TELEPHONE",
          detail = "01234 567890"
        ),
        IfContactDetail(
          code = 9,
          detailType = "MOBILE TELEPHONE",
          detail = "01234 567890"
        )
      )
    ),
    residences = Some(
      Seq(
        IfResidence(
          statusCode = Option("1"),
          status = Option("VERIFIED"),
          typeCode = Option(14),
          residenceType = Option("NOMINATED"),
          deliveryInfo = None,
          retLetterServ = None,
          addressCode = Option("1"),
          addressType = Option("UK"),
          address = Option(
            IfAddress(
              line1 = Option("24 Trinity Street"),
              line2 = Option("Dawley Bank"),
              line3 = Option("Telford"),
              line4 = Option("Shropshire"),
              line5 = Option("UK"),
              postcode = Option("TF3 4ER")
            )
          ),
          houseId = None,
          homeCountry = None,
          otherCountry = None,
          deadLetterOfficeDate = None,
          startDateTime = None,
          noLongerUsed = Option("N")
        ),
        IfResidence(
          statusCode = Option("3"),
          status = Option("AS INPUT"),
          typeCode = Option(13),
          residenceType = Option("BASE"),
          deliveryInfo = None,
          retLetterServ = None,
          addressCode = Option("2"),
          addressType = Option("NON-UK"),
          address = Option(
            IfAddress(
              line1 = Option("La Petite Maison"),
              line2 = Option("Rue de Bastille"),
              line3 = Option("Vieux Ville"),
              line4 = Option("Dordogne"),
              line5 = Option("France"),
              postcode = None
            )
          ),
          houseId = None,
          homeCountry = None,
          otherCountry = None,
          deadLetterOfficeDate = None,
          startDateTime = None,
          noLongerUsed = Option("Y")
        )
      )
    )
  )
}
