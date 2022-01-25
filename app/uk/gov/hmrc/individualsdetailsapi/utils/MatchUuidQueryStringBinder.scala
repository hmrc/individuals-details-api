/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.individualsdetailsapi.utils

import java.util.UUID

import play.api.mvc.QueryStringBindable

import scala.util.Try

class MatchUuidQueryStringBinder extends QueryStringBindable[UUID] {

  private val parameterName = "matchId"

  override def bind(key: String, params: Map[String, Seq[String]]) = {
    Option(Try(params.get(parameterName) flatMap (_.headOption) match {
      case Some(parameterValue) =>
        UuidValidator.validate(parameterValue) match {
          case true => Right(UUID.fromString(parameterValue))
          case false => Left(s"$parameterName format is invalid")
        }
      case None                 => Left(s"$parameterName is required")
    }) getOrElse Left(s"$parameterName format is invalid"))
  }

  override def unbind(key: String, uuid: UUID) = s"$key=${uuid.toString}"

}
