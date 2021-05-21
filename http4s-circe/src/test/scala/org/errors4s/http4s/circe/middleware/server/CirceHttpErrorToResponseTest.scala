package org.errors4s.http4s.circe.middleware.server

import cats.data._
import cats.effect._
import cats.implicits._
import org.errors4s.core.syntax.all._
import org.errors4s.http._
import org.errors4s.http.circe._
import org.errors4s.http4s.circe._
import org.http4s._
import org.http4s.headers._
import org.scalatest.Assertion

final class CirceHttpErrorToResponseTest extends BaseTest {
  private def failingHttpRoutesJson(t: Throwable): HttpRoutes[SyncIO] =
    CirceHttpErrorToResponse
      .json(Sync[SyncIO])(Kleisli(Function.const(OptionT(SyncIO.raiseError[Option[Response[SyncIO]]](t)))))
  private val testRequest: Request[SyncIO] = Request(method = Method.GET)

  "CirceHttpErrorToResponse.json middleware" should
    "yield a application/json+problem with a 501 status when a specific ExtensibleCirceHttpError is raised" in
    sio {
      val error: ExtensibleCirceHttpError = ExtensibleCirceHttpError
        .simple(nes"about:blank", nes"Blank Error", HttpStatus(501), None, None)

      failingHttpRoutesJson(error)
        .run(testRequest)
        .value
        .flatMap(
          _.fold(SyncIO[Assertion](fail("No Response"))) { resp =>
            SyncIO(resp.status.code shouldBe 501) *>
              SyncIO(
                resp.headers.get(`Content-Type`) shouldBe Some(`Content-Type`(MediaType.application.`problem+json`))
              ) *> resp.as[ExtensibleCirceHttpError].flatMap(value => SyncIO(value shouldBe error))
          }
        )
    }

  it should "yield a application/json+problem with a 501 status when a specific ExtensibleCirceHttpProblem is raised" in
    sio {
      val error: ExtensibleCirceHttpProblem = ExtensibleCirceHttpProblem
        .simple(Some("about:blank"), Some("Blank Error"), Some(501), None, None)

      failingHttpRoutesJson(error)
        .run(testRequest)
        .value
        .flatMap(
          _.fold(SyncIO[Assertion](fail("No Response"))) { resp =>
            SyncIO(resp.status.code shouldBe 501) *>
              SyncIO(
                resp.headers.get(`Content-Type`) shouldBe Some(`Content-Type`(MediaType.application.`problem+json`))
              ) *> resp.as[ExtensibleCirceHttpProblem].flatMap(value => SyncIO(value shouldBe error))
          }
        )
    }
}
