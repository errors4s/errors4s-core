package io.isomarcte.errors4s.http4s.circe.middleware

import cats.data._
import cats.effect._
import cats.implicits._
import eu.timepit.refined.types.all._
import io.isomarcte.errors4s.http._
import io.isomarcte.errors4s.http.circe.CirceHttpError._
import io.isomarcte.errors4s.http.circe.CirceHttpProblem._
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
      implicit val ed: EntityDecoder[SyncIO, SimpleCirceHttpError] = simpleCirceHttpErrorJsonEntityDecoder
      val error: SimpleCirceHttpError = SimpleCirceHttpError(
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
              ) *> resp.as[SimpleCirceHttpError].flatMap(value => SyncIO(value shouldBe error))
          }
        )
    }

  it should "yield a application/json+problem with a 501 status when a specific CirceHttpProblem is raised" in
    sio {
      implicit val ed: EntityDecoder[SyncIO, SimpleCirceHttpProblem] = simpleCirceHttpProblemJsonEntityDecoder
      val error: SimpleCirceHttpProblem = SimpleCirceHttpProblem(
        Some("about:blank"),
        Some("Blank Error"),
        Some(501),
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
              ) *> resp.as[SimpleCirceHttpProblem].flatMap(value => SyncIO(value shouldBe error))
          }
        )
    }
}
