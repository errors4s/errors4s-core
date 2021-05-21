package org.errors4s.http4s.circe.middleware.client

import cats.effect._
import cats.effect.concurrent._
import cats.implicits._
import fs2.Chunk
import fs2.Stream
import io.circe._
import io.circe.syntax._
import java.nio.charset.StandardCharsets
import org.errors4s.circe.instances._
import org.errors4s.core._
import org.errors4s.http._
import org.errors4s.http.circe._
import org.errors4s.http.circe.implicits.httpStatusCodec
import org.errors4s.http4s.circe._
import org.http4s._
import org.http4s.client._
import org.http4s.headers._

final class PassthroughCirceHttpErrorTest extends BaseTest {
  import PassthroughCirceHttpErrorTest._

  private def bodyAsChunk(value: Response[SyncIO]): SyncIO[Chunk[Byte]] =
    value.body.chunks.foldMonoid(Chunk.instance.algebra[Byte]).compile.lastOrError

  private def compareResponses(value: Response[SyncIO], expected: Response[SyncIO]): SyncIO[Unit] =
    SyncIO(value.status shouldBe expected.status) *> SyncIO(value.headers shouldBe expected.headers) *>
      bodyAsChunk(value)
        .flatMap(value => bodyAsChunk(expected).flatMap(expected => SyncIO(value shouldBe expected).void))

  "A response with a application/problem+json Content-Type" should "raise an error in F if the body is well formed" in
    sio {
      val client: Client[SyncIO] = constClientF(httpErrorAsResponse[SyncIO](testError))

      client
        .run(testRequest)
        .use[SyncIO, Unit](resp => SyncIO(fail(s"Expected error, but got $resp")))
        .attemptT
        .foldF(
          e =>
            e match {
              case e: ExtensibleCirceHttpProblem =>
                SyncIO(e.asJson shouldBe testError.asJson)
              case otherwise =>
                SyncIO(fail(s"Expected ExtensibleCirceHttpProblem, but got $otherwise"))
            },
          value => SyncIO(fail(s"Expected error, but got successful $value"))
        )
    }

  it should "yield the original response if parsing fails (e.g. the Content-Type is wrong)" in
    sio {
      val client: Client[SyncIO] = constClient(responseWithContentType)

      client.run(testRequest).use[SyncIO, Unit](resp => compareResponses(resp, responseWithContentType))
    }

  "The PassthroughCirceHttpError middleware" should
    "only read the body if there is an application/problem+json, and in that case it should only do so once" in
    sio {
      Ref
        .of[SyncIO, Int](0)
        .flatMap { ref =>
          val responseBody: String = "Body"
          val response: Response[SyncIO] = Response(
            status = Status.Ok,
            headers = Headers.of(`Content-Type`(MediaType.application.`problem+json`)),
            body = Stream
              .evalUnChunk(ref.update(_ + 1) *> SyncIO.pure(Chunk.bytes(responseBody.getBytes(StandardCharsets.UTF_8))))
          )

          val client: Client[SyncIO] = constClient(response)

          // The result should be _2_ and not _1_ because the body is evaluated
          // once in the middleware and once again by the `compareResponses`
          // helper (which uses the original `fs2.Stream` value.
          client.run(testRequest).use(value => compareResponses(value, response)) *>
            ref.get.flatMap(value => SyncIO(value shouldBe 2))
        }
    }

  it should "not read the body at all if the content-type is not an application/problem+json" in
    sio {
      Ref
        .of[SyncIO, Int](0)
        .flatMap { ref =>
          val responseBody: String = "Body"
          val response: Response[SyncIO] = Response(
            status = Status.Ok,
            body = Stream
              .evalUnChunk(ref.update(_ + 1) *> SyncIO.pure(Chunk.bytes(responseBody.getBytes(StandardCharsets.UTF_8))))
          )

          val client: Client[SyncIO] = constClient(response)

          client
            .run(testRequest)
            .use(value =>
              // Throw away the body, which should prevent the Ref from ever being
              // updated if the middleware is working correctly.
              SyncIO.pure(value.copy(body = Stream.empty))
            ) *> ref.get.flatMap(value => SyncIO(value shouldBe 0))
        }
    }
}

object PassthroughCirceHttpErrorTest {
  final private case class CustomCirceHttpError(
    override val `type`: NonEmptyString,
    override val title: NonEmptyString,
    override val status: HttpStatus,
    override val detail: Option[String],
    override val instance: Option[NonEmptyString],
    customInt: Int
  ) extends ExtensibleCirceHttpError {
    override lazy val additionalFields: JsonObject = JsonObject("customInt" -> customInt.asJson)
  }

  private object CustomCirceHttpError {
    implicit lazy val c: Codec[CustomCirceHttpError] =
      Codec.forProduct6("type", "title", "status", "detail", "instance", "customInt")(CustomCirceHttpError.apply _)(
        (value: CustomCirceHttpError) =>
          (value.`type`, value.title, value.status, value.detail, value.instance, value.customInt)
      )
  }

  private lazy val testError: CustomCirceHttpError = CustomCirceHttpError(
    NonEmptyString("about:blank"),
    NonEmptyString("Title"),
    HttpStatus(501),
    None,
    None,
    1
  )

  private lazy val testRequest: Request[SyncIO] = Request()

  private lazy val responseWithContentType: Response[SyncIO] = Response(headers =
    Headers.of(`Content-Type`(MediaType.application.`problem+json`))
  )

  private def constClientF(response: SyncIO[Response[SyncIO]]): Client[SyncIO] =
    PassthroughCirceHttpError(Sync[SyncIO])(Client[SyncIO](Function.const(Resource.eval(response))))

  private def constClient(response: Response[SyncIO]): Client[SyncIO] = constClientF(SyncIO.pure(response))
}
