package io.isomarcte.errors4s.http4s.circe.middleware

import cats.data._
import cats.effect._
import cats.implicits._
import eu.timepit.refined.types.all._
import io.isomarcte.errors4s.http.HttpError._
import io.isomarcte.errors4s.http.HttpProblem._
import io.isomarcte.errors4s.http._
import io.isomarcte.errors4s.http4s.circe._
import org.http4s._
import org.http4s.headers._
import org.scalatest.Assertion

final class CirceHttpErrorToResponseTest extends BaseTest {
  private def failingHttpRoutesJson(t: Throwable): HttpRoutes[SyncIO] =
    CirceHttpErrorToResponse
      .json(Sync[SyncIO])(Kleisli(Function.const(OptionT(SyncIO.raiseError[Option[Response[SyncIO]]](t)))))
  private val testRequest: Request[SyncIO] = Request(method = Method.GET)

  "CirceHttpErrorToResponse.json middleware" should
    "yield a application/json+problem with a 501 status when a specific CirceHttpError is raised" in
    sio {
      val error: SimpleHttpError = SimpleHttpError(
        NonEmptyString("about:blank"),
        NonEmptyString("Blank Error"),
        HttpStatus(501),
        None,
        None
      )

      failingHttpRoutesJson(error)
        .run(testRequest)
        .value
        .flatMap(
          _.fold(SyncIO[Assertion](fail("No Response"))) { resp =>
            SyncIO(resp.status.code shouldBe 501) *>
              SyncIO(
                resp.headers.get(`Content-Type`) shouldBe Some(`Content-Type`(MediaType.application.`problem+json`))
              ) *> resp.as[SimpleHttpError].flatMap(value => SyncIO(value shouldBe error))
          }
        )
    }

  it should "yield a application/json+problem with a 501 status when a specific CirceHttpProblem is raised" in
    sio {
      val error: SimpleHttpProblem = SimpleHttpProblem(Some("about:blank"), Some("Blank Error"), Some(501), None, None)

      failingHttpRoutesJson(error)
        .run(testRequest)
        .value
        .flatMap(
          _.fold(SyncIO[Assertion](fail("No Response"))) { resp =>
            SyncIO(resp.status.code shouldBe 501) *>
              SyncIO(
                resp.headers.get(`Content-Type`) shouldBe Some(`Content-Type`(MediaType.application.`problem+json`))
              ) *> resp.as[SimpleHttpProblem].flatMap(value => SyncIO(value shouldBe error))
          }
        )
    }
}
