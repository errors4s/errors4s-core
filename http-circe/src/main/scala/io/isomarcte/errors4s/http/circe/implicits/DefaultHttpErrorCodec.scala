package io.isomarcte.errors4s.http.circe.implicits

import io.circe._
import io.circe.generic.semiauto._
import io.circe.refined._
import io.circe.syntax._
import io.isomarcte.errors4s.http._
import io.isomarcte.errors4s.http.circe._

trait DefaultHttpErrorCodec {
  import HttpError._

  implicit def simpleHttpErrorCodec: Codec[SimpleHttpError] = Codec.from[SimpleHttpError](deriveDecoder, deriveEncoder)

  implicit def httpErrorCodec: Codec[HttpError] =
    Codec.from[HttpError](
      Decoder[SimpleHttpError].map(identity),
      Encoder.instance[HttpError] {
        case value: ExtensibleCirceHttpError =>
          value.toJson
        case value =>
          SimpleHttpError(value.`type`, value.title, value.status, value.detail, value.instance).asJson
      }
    )
}

object DefaultHttpErrorCodec extends DefaultHttpErrorCodec
