package org.errors4s.http4s.circe.middleware.server

import cats.data._
import cats.effect._
import cats.implicits._
import org.errors4s.http._
import org.errors4s.http4s.circe._
import org.http4s._
import org.http4s.server._

object CirceHttpErrorToResponse {

  /** A middleware which catches `HttpError` or `HttpProblem` converts them in
    * to the appropriate `Response` values. Other [[java.lang.Throwable]] values are
    * ignored, e.g. they are not caught.
    *
    * The recommended usage is to have your application raise errors which for
    * which you intend to return an error `Response` as `HttpError`
    * values. Errors which should not have a `Response` body should be raised
    * as something else, e.g. `Error or `RuntimeException`.
    */
  def json[F[_]](implicit F: Sync[F]): HttpMiddleware[F] = { (service: HttpRoutes[F]) =>
    HttpRoutes((request: Request[F]) =>
      service
        .run(request)
        .recoverWith {
          case e: HttpError =>
            OptionT.liftF(httpErrorAsResponse[F](e))
          case e: HttpProblem =>
            OptionT.liftF(httpProblemAsResponse[F](e))
        }
    )
  }
}
