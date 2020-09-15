package io.isomarcte.errors4s.http.circe

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.isomarcte.errors4s.http._

trait CirceHttpProblem extends HttpProblem {

  /** Convert this [[CirceHttpProblem]] to JSON
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

object CirceHttpProblem {

  /** The trivial implementation of [[CirceHttpProblem]].
    *
    * @note [[CirceHttpError#SimpleCirceHttpError]], or a subtype thereof, is
    *       recommended when throwing new errors.
    */
  final case class SimpleCirceHttpProblem(
    override val `type`: Option[String],
    override val title: Option[String],
    override val status: Option[Int],
    override val detail: Option[String],
    override val instance: Option[String]
  ) extends CirceHttpProblem {
    final override lazy val toJson: Json = this.asJson
  }

  object SimpleCirceHttpProblem {
    implicit val c: Codec[SimpleCirceHttpProblem] = Codec.from(deriveDecoder, deriveEncoder)
  }
}
