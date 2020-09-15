package io.isomarcte.errors4s.http4s

import cats.effect._
import io.circe._
import io.isomarcte.errors4s.http.circe.CirceHttpError._
import io.isomarcte.errors4s.http.circe.CirceHttpProblem._
import io.isomarcte.errors4s.http.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.headers._

package object circe {
  def circeHttpProblemJsonEntityEncoder[F[_]]: EntityEncoder[F, CirceHttpProblem] =
    EntityEncoder[F, Json]
      .contramap((value: CirceHttpProblem) => value.toJson)
      .withContentType(`Content-Type`(MediaType.application.`problem+json`))

  def simpleCirceHttpProblemJsonEntityEncoder[F[_]]: EntityEncoder[F, SimpleCirceHttpProblem] =
    circeHttpProblemJsonEntityEncoder[F].contramap(identity)

  def simpleCirceHttpProblemJsonEntityDecoder[F[_]: Sync]: EntityDecoder[F, SimpleCirceHttpProblem] =
    jsonOf[F, CirceHttpProblem.SimpleCirceHttpProblem]

  def circeHttpProblemJsonEntityDecoder[F[_]: Sync]: EntityDecoder[F, CirceHttpProblem] =
    circeHttpProblemJsonEntityDecoder[F].widen

  def circeHttpErrorJsonEntityEncoder[F[_]]: EntityEncoder[F, CirceHttpError] =
    EntityEncoder[F, Json]
      .contramap((value: CirceHttpError) => value.toJson)
      .withContentType(`Content-Type`(MediaType.application.`problem+json`))

  def simpleCirceHttpErrorJsonEntityEncoder[F[_]]: EntityEncoder[F, SimpleCirceHttpError] =
    circeHttpErrorJsonEntityEncoder.contramap(identity)

  def simpleCirceHttpErrorJsonEntityDecoder[F[_]: Sync]: EntityDecoder[F, SimpleCirceHttpError] =
    jsonOf[F, CirceHttpError.SimpleCirceHttpError]

  def circeHttpErrorJsonEntityDecoder[F[_]: Sync]: EntityDecoder[F, CirceHttpError] =
    simpleCirceHttpErrorJsonEntityDecoder[F].widen
}
