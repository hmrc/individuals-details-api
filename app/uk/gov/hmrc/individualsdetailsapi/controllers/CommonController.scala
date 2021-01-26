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

package uk.gov.hmrc.individualsdetailsapi.controllers
import javax.inject.Inject
import play.api.mvc.{ControllerComponents, Result}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{
  AuthorisationException,
  AuthorisedFunctions,
  Enrolment,
  InsufficientEnrolments
}
import uk.gov.hmrc.http.{HeaderCarrier, TooManyRequestException}
import uk.gov.hmrc.individualsdetailsapi.domain._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

abstract class CommonController @Inject()(
    cc: ControllerComponents
) extends BackendController(cc) {

  private[controllers] def recovery: PartialFunction[Throwable, Result] = {
    case _: MatchNotFoundException => ErrorNotFound.toHttpResponse
    case _: InsufficientEnrolments =>
      ErrorUnauthorized("User does not have valid scopes").toHttpResponse
    case e: AuthorisationException =>
      ErrorUnauthorized(e.getMessage).toHttpResponse
    case _: TooManyRequestException => ErrorTooManyRequests.toHttpResponse
    case e: IllegalArgumentException =>
      ErrorInvalidRequest(e.getMessage).toHttpResponse
    case x => ErrorInvalidRequest(x.getMessage).toHttpResponse
  }
}

trait PrivilegedAuthentication extends AuthorisedFunctions {

  val environment: String

  def authPredicate(scopes: Iterable[String]): Predicate =
    scopes.map(Enrolment(_): Predicate).reduce(_ or _)

  def requiresPrivilegedAuthentication(endpointScopes: Iterable[String])(
      f: Iterable[String] => Future[Result])(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Result] = {

    if (endpointScopes.isEmpty) throw new Exception("No scopes defined")

    if (environment == Environment.SANDBOX)
      f(endpointScopes.toList)
    else {
      authorised(authPredicate(endpointScopes))
        .retrieve(Retrievals.allEnrolments) {
          case scopes => f(scopes.enrolments.map(e => e.key).toList)
        }
    }
  }
}

object Environment {
  val SANDBOX = "SANDBOX"
  val PRODUCTION = "PRODUCTION"
}
