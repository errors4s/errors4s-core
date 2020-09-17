package io.isomarcte.errors4s.http.circe

import io.circe._
import io.isomarcte.errors4s.http._
import io.isomarcte.errors4s.http.circe.implicits._

trait ExtensibleCirceHttpProblem extends HttpProblem {

  /** Convert this [[ExtensibleCirceHttpProblem]] to JSON
    *
    * @note We can't just use a `Encoder[CirceHttpProblem]` here because the
    *       actual implementation may have extension members as defined in RFC
    *       7807. Thus we have to force the concrete implementation to provide
    *       the real JSON representation.
    *
    * @see [[https://tools.ietf.org/html/rfc7807#section-3.2]]
    */
  def toJson: Json
}

object ExtensibleCirceHttpProblem {

  implicit def circeCodec: Codec[ExtensibleCirceHttpProblem] =
    Codec.from[ExtensibleCirceHttpProblem](
      Decoder.instance[ExtensibleCirceHttpProblem]((hcursor: HCursor) =>
        Decoder[HttpProblem.SimpleHttpProblem]
          .apply(hcursor)
          .map(sht =>
            new ExtensibleCirceHttpProblem {
              override val `type`: Option[String]   = sht.`type`
              override val title: Option[String]    = sht.title
              override val status: Option[Int]      = sht.status
              override val detail: Option[String]   = sht.detail
              override val instance: Option[String] = sht.instance
              override val toJson: Json             = hcursor.value
            }
          )
      ),
      Encoder.instance[ExtensibleCirceHttpProblem](_.toJson)
    )
}
