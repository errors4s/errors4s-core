package io.isomarcte.errors4s.http4s.circe.middleware

import cats.effect._
import org.http4s.server._

object CirceHttpErrorToResponse {

  /** A middleware which catches `HttpError` or `HttpProblem` converts them in
    * to the appropriate `Response` values. Other `Throwable` values are
    * ignored, e.g. they are not caught.
    *
    * The recommended usage is to have your application raise errors which for
    * which you intend to return an error `Response` as `HttpError`
    * values. Errors which should not have a `Response` body should be raised
    * as something else, e.g. `Error or `RuntimeException`.
    */
  @deprecated(
    message = "Please use io.isomarcte.errors4s.http4s.circe.middleware.server.CirceHttpErrorToResponse",
    since = "0.0.3"
  )
  def json[F[_]](implicit F: Sync[F]): HttpMiddleware[F] = server.CirceHttpErrorToResponse.json[F]
}
