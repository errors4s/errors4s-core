package io.isomarcte.errors4s.http.circe

import eu.timepit.refined.types.all._
import io.circe._
import io.isomarcte.errors4s.http._
import io.isomarcte.errors4s.http.circe.implicits._

trait ExtensibleCirceHttpError extends HttpError {

  /** Convert this [[ExtensibleCirceHttpError]] to JSON
    *
    * @note We can't just use a `Encoder[ExtensibleCirceHttpError]` here because the
    *       actual implementation may have extension members as defined in RFC
    *       7807. Thus we have to force the concrete implementation to provide
    *       the real JSON representation.
    *
    * @see [[https://tools.ietf.org/html/rfc7807#section-3.2]]
    */
  def toJson: Json
}

object ExtensibleCirceHttpError {

  implicit def circeCodec: Codec[ExtensibleCirceHttpError] =
    Codec.from[ExtensibleCirceHttpError](
      Decoder.instance[ExtensibleCirceHttpError]((hcursor: HCursor) =>
        Decoder[HttpError.SimpleHttpError]
          .apply(hcursor)
          .map(sht =>
            new ExtensibleCirceHttpError {
              override val `type`: NonEmptyString           = sht.`type`
              override val title: NonEmptyString            = sht.title
              override val status: HttpStatus               = sht.status
              override val detail: Option[NonEmptyString]   = sht.detail
              override val instance: Option[NonEmptyString] = sht.instance
              override val toJson: Json                     = hcursor.value
            }
          )
      ),
      Encoder.instance[ExtensibleCirceHttpError](_.toJson)
    )
}
