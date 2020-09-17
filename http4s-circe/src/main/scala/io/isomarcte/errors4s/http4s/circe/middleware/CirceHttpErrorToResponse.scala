package io.isomarcte.errors4s.http4s.circe.middleware

import cats.data._
import cats.effect._
import cats.implicits._
import io.isomarcte.errors4s.http.circe._
import io.isomarcte.errors4s.http4s.circe._
import org.http4s._
import org.http4s.server._

object CirceHttpErrorToResponse {

  /** A middleware which catches `CirceHttpError` or `CirceHttpProblem` converts them in
    * to the appropriate `Response` values. Other `Throwable` values are
    * ignored, e.g. they are not caught.
    *
    * The recommended usage is to have your application raise errors which for
    * which you intend to return an error `Response` as `HttpError`
    * values. Errors which should not have a `Response` body should be raised
    * as something else, e.g. `Error or `RuntimeException`.
    */
  def json[F[_]](implicit F: Sync[F]): HttpMiddleware[F] = { (service: HttpRoutes[F]) =>
    HttpRoutes((request: Request[F]) =>
      OptionT(
        service
          .run(request)
          .value
          .recoverWith {
            case e: CirceHttpError =>
              F.pure(Some(Response(status = Status(e.status.value)).withEntity(e)(circeHttpErrorJsonEntityEncoder[F])))
            case e: CirceHttpProblem =>
              F.pure(
                Some(
                  Response(status = e.status.fold(Status.InternalServerError)(value => Status(value)))
                    .withEntity(e)(circeHttpProblemJsonEntityEncoder[F])
                )
              )
          }
      )
    )
  }
}
