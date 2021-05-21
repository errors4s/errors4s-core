package org.errors4s.circe

import io.circe._
import org.errors4s.core._

private[circe] trait NonEmptyStringInstances {

  implicit final lazy val nesCodec: Codec[NonEmptyString] = Codec
    .from(Decoder[String].emap(NonEmptyString.from), Encoder[String].contramap(_.value))
}
