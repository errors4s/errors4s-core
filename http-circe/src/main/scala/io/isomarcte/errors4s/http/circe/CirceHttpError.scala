package io.isomarcte.errors4s.http.circe

import eu.timepit.refined.types.all._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.refined._
import io.circe.syntax._
import io.isomarcte.errors4s.http._

trait CirceHttpError extends HttpError {

  /** Convert this [[CirceHttpError]] to JSON
    *
    * @note We can't just use a `Encoder[CirceHttpError]` here because the
    *       actual implementation may have extension members as defined in RFC
    *       7807. Thus we have to force the concrete implementation to provide
    *       the real JSON representation.
    *
    * @see [[https://tools.ietf.org/html/rfc7807#section-3.2]]
    */
  def toJson: Json
}

object CirceHttpError {

  /** The trivial implementation of [[CirceHttpError]]. */
  final case class SimpleCirceHttpError(
    override val `type`: NonEmptyString,
    override val title: NonEmptyString,
    override val status: HttpStatus,
    override val detail: Option[NonEmptyString],
    override val instance: Option[NonEmptyString]
  ) extends CirceHttpError {
    final override lazy val toJson: Json = this.asJson
  }

  object SimpleCirceHttpError {
    implicit final lazy val c: Codec[SimpleCirceHttpError] = Codec.from(deriveDecoder, deriveEncoder)
  }
}
