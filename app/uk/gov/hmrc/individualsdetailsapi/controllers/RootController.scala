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

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.mvc.hal._
import play.api.hal.HalLink
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsdetailsapi.service.{ScopesHelper, ScopesService}
import uk.gov.hmrc.individualsdetailsapi.services.{
  DetailsService,
  LiveDetailsService,
  SandboxDetailsService
}

import scala.concurrent.ExecutionContext

abstract class RootController @Inject()(
    cc: ControllerComponents,
    scopeService: ScopesService,
    scopesHelper: ScopesHelper,
    detailsService: DetailsService)(implicit val ec: ExecutionContext)
    extends CommonController(cc)
    with PrivilegedAuthentication {

  def root(matchId: UUID): Action[AnyContent] = Action.async {
    implicit request =>
      {
        requiresPrivilegedAuthentication(scopeService.getAllScopes) {
          authScopes =>
            detailsService.resolve(matchId) map { _ =>
              val selfLink =
                HalLink("self", s"/individuals/employments/?matchId=$matchId")
              Ok(scopesHelper.getHalLinks(matchId, authScopes) ++ selfLink)
            }
        } recover recovery
      }
  }
}

@Singleton
class LiveRootController @Inject()(
    val authConnector: AuthConnector,
    cc: ControllerComponents,
    scopeService: ScopesService,
    scopesHelper: ScopesHelper,
    detailsService: LiveDetailsService
)(override implicit val ec: ExecutionContext)
    extends RootController(cc, scopeService, scopesHelper, detailsService) {

  override val environment = Environment.PRODUCTION
}

@Singleton
class SandboxRootController @Inject()(
    val authConnector: AuthConnector,
    cc: ControllerComponents,
    scopeService: ScopesService,
    scopesHelper: ScopesHelper,
    detailsService: SandboxDetailsService
)(override implicit val ec: ExecutionContext)
    extends RootController(cc, scopeService, scopesHelper, detailsService) {

  override val environment = Environment.SANDBOX
}
