package org.errors4s.http.circe.implicits

import io.circe._
import org.errors4s.http._
import org.errors4s.http.circe._

trait DefaultHttpProblemCodec {

  implicit def httpProblemCodec: Codec[HttpProblem] =
    Codec.from[HttpProblem](
      Decoder[ExtensibleCirceHttpProblem].map(identity),
      Encoder[ExtensibleCirceHttpProblem].contramap(ExtensibleCirceHttpProblem.fromHttpProblem)
    )
}

object DefaultHttpProblemCodec extends DefaultHttpProblemCodec
