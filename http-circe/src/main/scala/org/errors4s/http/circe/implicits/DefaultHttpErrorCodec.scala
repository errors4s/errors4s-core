package org.errors4s.http.circe.implicits

import io.circe._
import org.errors4s.http._
import org.errors4s.http.circe._

trait DefaultHttpErrorCodec {

  implicit final lazy val httpErrorCodec: Codec[HttpError] = Codec.from[HttpError](
    Decoder[ExtensibleCirceHttpError].map(identity),
    Encoder[ExtensibleCirceHttpError].contramap(ExtensibleCirceHttpError.fromHttpError)
  )
}

object DefaultHttpErrorCodec extends DefaultHttpErrorCodec
