package org.errors4s.http.circe.implicits

import io.circe._
import org.errors4s.http._

private[implicits] trait HttpStatusInstances {

  implicit final lazy val httpStatusCodec: Codec[HttpStatus] = Codec
    .from(Decoder[Int].emap(HttpStatus.from), Encoder[Int].contramap(_.value))
}
