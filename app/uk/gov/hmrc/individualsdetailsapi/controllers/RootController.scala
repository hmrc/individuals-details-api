/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsdetailsapi.service.ScopesService

import scala.concurrent.ExecutionContext.Implicits.global

abstract class RootController @Inject()(
    cc: ControllerComponents,
    scopeService: ScopesService
) extends CommonController(cc)
    with PrivilegedAuthentication {

  def root(): Action[AnyContent] = Action.async { implicit request =>
    val scopes =
      scopeService.getEndPointScopes("/individuals/benefits-and-credits")

    requiresPrivilegedAuthentication(scopes)
      .flatMap { authScopes =>
        //TODO:- add actual scopes
        throw new Exception("NOT_IMPLEMENTED")
      }
      .recover(recovery)

  }

}
@Singleton
class LiveRootController @Inject()(
    val authConnector: AuthConnector,
    cc: ControllerComponents,
    scopeService: ScopesService
) extends RootController(cc, scopeService) {

  override val environment = Environment.PRODUCTION
}

@Singleton
class SandboxRootController @Inject()(
    val authConnector: AuthConnector,
    cc: ControllerComponents,
    scopeService: ScopesService
) extends RootController(cc, scopeService) {

  override val environment = Environment.SANDBOX
}
