package io.isomarcte.errors4s.http.circe.implicits

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.isomarcte.errors4s.http._
import io.isomarcte.errors4s.http.circe._

trait DefaultHttpProblemCodec {
  import HttpProblem._

  implicit def simpleHttpProblemCodec: Codec[SimpleHttpProblem] =
    Codec.from[SimpleHttpProblem](deriveDecoder, deriveEncoder)

  implicit def httpProblemCodec: Codec[HttpProblem] =
    Codec.from[HttpProblem](
      Decoder[SimpleHttpProblem].map(identity),
      Encoder.instance[HttpProblem] {
        case value: ExtensibleCirceHttpProblem =>
          value.toJson
        case value =>
          SimpleHttpProblem(value.`type`, value.title, value.status, value.detail, value.instance).asJson
      }
    )
}

object DefaultHttpProblemCodec extends DefaultHttpProblemCodec
