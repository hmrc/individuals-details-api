package uk.gov.hmrc.individualsdetailsapi.domains

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.individualsdetailsapi.domains.integrationframework.IfContactDetail

// TODO: Our model is assuming that there will only be one of each telephone number type... check this?

case class ContactDetails( daytimeTelephone: Option[String],
                           eveningTelephone: Option[String],
                           mobileTelephone: Option[String] )

object ContactDetails {

  implicit val format: Format[ContactDetails] = Json.format[ContactDetails]

  private val DaytimeTelephone: Int = 7
  private val EveningTelephone: Int = 8
  private val MobileTelephone: Int = 9

  def create( daytimeTelephone: Option[String],
              eveningTelephone: Option[String],
              mobileTelephone: Option[String] ): Option[ContactDetails] =
    (daytimeTelephone, eveningTelephone, mobileTelephone) match {
      case (None, None, None) => None
      case _                  => Option(new ContactDetails(daytimeTelephone, eveningTelephone, mobileTelephone))
    }

  def create( contactDetails: Option[Seq[IfContactDetail]]): Option[ContactDetails] =
    contactDetails.flatMap { cdSeq =>
      cdSeq.foldLeft((None[String], None[String], None[String]))((tuple, detail) =>
        detail.code match {
          case DaytimeTelephone => tuple.copy(_1 = Some(detail.detail))
          case EveningTelephone => tuple.copy(_2 = Some(detail.detail))
          case MobileTelephone  => tuple.copy(_3 = Some(detail.detail))
          case _ => tuple
        }) match {
          case (None, None, None) => None
          case (daytimeTelephone, eveningTelephone, mobileTelephone) =>
            create(daytimeTelephone, eveningTelephone, mobileTelephone)
      }
    }
}
