package uk.gov.hmrc.individualsdetailsapi.domains

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.individualsdetailsapi.domains.integrationframework.IfAddress

case class Address( line1: Option[String],
                    line2: Option[String],
                    line3: Option[String],
                    line4: Option[String],
                    line5: Option[String],
                    postcode: Option[String] )

object Address {

  implicit val addressFormat: Format[Address] = Json.format[Address]

  def create(line1: Option[String],
             line2: Option[String],
             line3: Option[String],
             line4: Option[String],
             line5: Option[String],
             postcode: Option[String]): Option[Address] =

    (line1, line2, line3, line4, line5, postcode) match {
      case (None, None, None, None, None, None) => None
      case _                                    => Some(new Address(line1, line2, line3, line4, line5, postcode))
    }

  def create(ifAddress: Option[IfAddress]): Option[Address] =
    ifAddress.flatMap(address => create(
      line1 = address.line1,
      line2 = address.line2,
      line3 = address.line3,
      line4 = address.line4,
      line5 = address.line5,
      postcode = address.postcode))
}
