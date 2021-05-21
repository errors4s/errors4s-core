package org.errors4s.http4s.circe.middleware.client

import cats.effect._
import cats.implicits._
import fs2.Chunk
import fs2.Stream
import org.errors4s.http.circe._
import org.http4s._
import org.http4s.circe.jsonOfWithMedia
import org.http4s.client._
import org.http4s.client.{Middleware => ClientMiddleware}
import org.http4s.headers._

/** Middlewares for raising RFC 7807 errors in client responses as errors in
  * some `F` type.
  */
object PassthroughCirceHttpError {
  private def shouldAttemptDecode(headers: Headers): Boolean =
    headers.get(`Content-Type`).fold(false)(ct => ct.mediaType === MediaType.application.`problem+json`)

  /** Middleware which looks for RFC 7807 errors by discriminating on the
    * `Content-Type` header. If that header is `application/problem+json`,
    * then it will attempt to decode the body as a `ExtensibleCirceHttpProblem`.
    *
    * If the `Content-Type` is missing or is any other value this middleware
    * does nothing to the `Response`.
    *
    * If the `Content-Type` ''is'' `application/problem+json`, but the decode
    * fails this should indicate a malformed `ExtensibleCirceHttpProblem`. In
    * that case the original `Response` is returned, but it is made
    * strict. This is necessarily to avoid attempting to re-stream the HTTP
    * response.
    */
  def apply[F[_]](implicit F: Sync[F]): ClientMiddleware[F] = { (client: Client[F]) =>
    Client[F]((request: Request[F]) =>
      client
        .run(request)
        .evalMap(resp =>
          if (shouldAttemptDecode(resp.headers)) {
            resp
              .body
              .chunks
              .foldMonoid(Chunk.instance.algebra[Byte])
              .compile
              .lastOrError
              .flatMap { (strictBody: Chunk[Byte]) =>
                val strictResp: Response[F] = resp.copy(body = Stream.chunk(strictBody))
                jsonOfWithMedia[F, ExtensibleCirceHttpProblem](MediaType.application.`problem+json`)
                  .decode(strictResp, true)
                  .foldF(
                    Function.const(F.pure(strictResp)),
                    (e: ExtensibleCirceHttpProblem) => F.raiseError[Response[F]](e)
                  )
              }
          } else {
            F.pure(resp)
          }
        )
    )
  }
}
