package io.isomarcte.errors4s.http4s

import cats.effect._
import io.circe._
import io.circe.syntax._
import io.isomarcte.errors4s.http.HttpError._
import io.isomarcte.errors4s.http.HttpProblem._
import io.isomarcte.errors4s.http.circe._
import io.isomarcte.errors4s.http.circe.implicits._
import io.isomarcte.errors4s.http.{circe => _, _}
import org.http4s._
import org.http4s.circe._
import org.http4s.headers._

package object circe {
  def circeHttpProblemJsonEntityEncoder[F[_]]: EntityEncoder[F, ExtensibleCirceHttpProblem] =
    EntityEncoder[F, Json]
      .contramap((value: ExtensibleCirceHttpProblem) => value.toJson)
      .withContentType(`Content-Type`(MediaType.application.`problem+json`))

  def circeHttpProblemJsonEntityDecoder[F[_]: Sync]: EntityDecoder[F, ExtensibleCirceHttpProblem] =
    circeHttpProblemJsonEntityDecoder[F].widen

  def circeHttpErrorJsonEntityEncoder[F[_]]: EntityEncoder[F, ExtensibleCirceHttpError] =
    EntityEncoder[F, Json]
      .contramap((value: ExtensibleCirceHttpError) => value.toJson)
      .withContentType(`Content-Type`(MediaType.application.`problem+json`))

  def circeHttpErrorJsonEntityDecoder[F[_]: Sync]: EntityDecoder[F, ExtensibleCirceHttpError] =
    EntityDecoder
      .decodeBy(MediaType.application.`problem+json`)(media => jsonOf[F, ExtensibleCirceHttpError].decode(media, false))

  implicit def simpleHttpErrorJsonEntityEncoder[F[_]]: EntityEncoder[F, SimpleHttpError] =
    EntityEncoder[F, Json]
      .contramap((sht: SimpleHttpError) => sht.asJson)
      .withContentType(`Content-Type`(MediaType.application.`problem+json`))

  implicit def simpleHttpErrorJsonEntityDecoder[F[_]: Sync]: EntityDecoder[F, SimpleHttpError] =
    EntityDecoder
      .decodeBy(MediaType.application.`problem+json`)(media => jsonOf[F, SimpleHttpError].decode(media, false))

  def httpErrorJsonEntityEncoder[F[_]]: EntityEncoder[F, HttpError] =
    simpleHttpErrorJsonEntityEncoder[F]
      .contramap(value => SimpleHttpError(value.`type`, value.title, value.status, value.detail, value.instance))

  def httpErrorJsonEntityDecoder[F[_]: Sync]: EntityDecoder[F, HttpError] = simpleHttpErrorJsonEntityDecoder[F].widen

  implicit def simpleHttpProblemJsonEntityEncoder[F[_]]: EntityEncoder[F, SimpleHttpProblem] =
    EntityEncoder[F, Json]
      .contramap((sht: SimpleHttpProblem) => sht.asJson)
      .withContentType(`Content-Type`(MediaType.application.`problem+json`))

  implicit def simpleHttpProblemJsonEntityDecoder[F[_]: Sync]: EntityDecoder[F, SimpleHttpProblem] =
    EntityDecoder
      .decodeBy(MediaType.application.`problem+json`)(media => jsonOf[F, SimpleHttpProblem].decode(media, false))

  def httpProblemJsonEntityEncoder[F[_]]: EntityEncoder[F, HttpProblem] =
    simpleHttpProblemJsonEntityEncoder[F]
      .contramap(value => SimpleHttpProblem(value.`type`, value.title, value.status, value.detail, value.instance))

  def httpProblemJsonEntityDecoder[F[_]: Sync]: EntityDecoder[F, HttpProblem] =
    simpleHttpProblemJsonEntityDecoder[F].widen

  /** Encode a `HttpError` as a http4s `Response.
    *
    * @note This respects `ExtensibleCirceHttpError` so if you have custom
    *       keys in your `HttpError` then ensure it extends
    *       `ExtensibleCirceHttpError`, otherwise they will not be detected.
    */
  def httpErrorAsResponse[F[_]](e: HttpError)(implicit F: Sync[F]): F[Response[F]] =
    e match {
      case e: ExtensibleCirceHttpError =>
        F.pure(Response(status = Status(e.status.value)).withEntity(e)(circeHttpErrorJsonEntityEncoder[F]))
      case _ =>
        F.pure(Response(status = Status(e.status.value)).withEntity(e)(httpErrorJsonEntityEncoder[F]))
    }

  /** Encode a `HttpProblem` as a http4s `Response.
    *
    * @note This respects `ExtensibleCirceHttpProblem` so if you have custom
    *       keys in your `HttpProblem` then ensure it extends
    *       `ExtensibleCirceHttpProblem`, otherwise they will not be detected.
    */
  def httpProblemAsResponse[F[_]](e: HttpProblem)(implicit F: Sync[F]): F[Response[F]] =
    e match {
      case e: ExtensibleCirceHttpProblem =>
        F.pure(
          Response(status = e.status.fold(Status.InternalServerError)(value => Status(value)))
            .withEntity(e)(circeHttpProblemJsonEntityEncoder[F])
        )
      case e: HttpProblem =>
        F.pure(
          Response(status = e.status.fold(Status.InternalServerError)(value => Status(value)))
            .withEntity(e)(httpProblemJsonEntityEncoder[F])
        )
    }
}
