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

package uk.gov.hmrc.individualsdetailsapi.audit.events

import java.util.UUID

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsdetailsapi.audit.models.ApiFailureEventModel
import uk.gov.hmrc.individualsdetailsapi.audit.HttpExtendedAuditEvent
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

abstract class FailureEventBase @Inject()(httpAuditEvent: HttpExtendedAuditEvent) {

  import httpAuditEvent.extendedDataEvent

  def auditType = "ApiFailureEvent"
  def transactionName = "AuditFail"
  def apiVersion = "1.0"

  def apply(correlationId: String,
            scopes: Option[String],
            matchId: Option[UUID],
            request: RequestHeader,
            requestUrl: Option[String],
            msg: String)(
      implicit hc: HeaderCarrier =
        HeaderCarrierConverter.fromHeadersAndSession(request.headers)
  ): ExtendedDataEvent = {

    val event = extendedDataEvent(
      auditType,
      transactionName,
      request,
      Json.toJson(
        ApiFailureEventModel(apiVersion,
                             matchId,
                             correlationId,
                             scopes,
                             requestUrl,
                             msg))
    )

    Logger.debug(s"$auditType - AuditEvent: $event")

    event

  }
}
