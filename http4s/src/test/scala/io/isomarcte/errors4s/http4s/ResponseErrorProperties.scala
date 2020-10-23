package io.isomarcte.errors4s.http4s

import cats.effect._
import cats.implicits._
import fs2._
import java.nio.charset.StandardCharsets
import org.http4s._
import org.http4s.client._
import org.http4s.laws.discipline.arbitrary._
import org.scalacheck.Prop.{Status => _, _}
import org.scalacheck._
import scodec.bits._

object ResponseErrorProperties extends Properties("ResponseError") {
  lazy val testRequest: Request[SyncIO] = Request()

  lazy val invalidStatusGen: Gen[Status] = Gen
    .choose(400, 599)
    .suchThat(s => s =!= Status.Gone.code && s =!= Status.NotFound.code)
    .map(status => Status(status))

  lazy val nonEmptyStringGen: Gen[String] = Gen.nonEmptyListOf(Arbitrary.arbitrary[Char]).map(_.mkString)

  property("FullErrorBody") =
    forAllNoShrink(
      Arbitrary.arbitrary[Headers],
      invalidStatusGen,
      nonEmptyStringGen,
      Arbitrary.arbitrary[Option[Uri]],
      Arbitrary.arbitrary[Option[Method]]
    ) { (headers: Headers, status: Status, errorBody: String, uri: Option[Uri], method: Option[Method]) =>
      val errorBodyBytes: ByteVector = ByteVector.view(errorBody.getBytes(StandardCharsets.UTF_8))
      val maxBodySize: Long          = errorBodyBytes.size + 1L
      val client: Client[SyncIO] = Client[SyncIO](
        Function.const(
          Resource.pure[SyncIO, Response[SyncIO]](
            Response(
              status = status,
              headers = headers,
              body = Stream.chunk(Chunk.bytes(errorBodyBytes.toArray)).covary
            )
          )
        )
      )

      client
        .expectOptionOr[String](testRequest)(ResponseError.fromResponse_(maxBodySize, uri, method))
        .attempt
        .map(
          _.fold(
            e =>
              e match {
                case e: ResponseError =>
                  ((e.status ?= status) :| "Expected Status") &&
                    ((e.headers ?= headers) :| "Expected Headers") &&
                    ((e.requestUri ?= uri) :| "Expected URI") &&
                    ((e.requestMethod ?= method) :| "Expected Method") &&
                    ((e.errorBody ?= ResponseError.ErrorBody.FullErrorBody(errorBodyBytes)) :| "Expected ErrorBody") &&
                    ((e.maxBodySize ?= maxBodySize) :| "Expected maxBodySize")
                case _ =>
                  exception(e)
              },
            r => (falsified :| s"Got successful response, should have got ResponseError: ${r}")
          )
        )
        .unsafeRunSync()
    }

  property("PartialErrorBody") =
    forAllNoShrink(
      Arbitrary.arbitrary[Headers],
      invalidStatusGen,
      nonEmptyStringGen,
      Arbitrary.arbitrary[Option[Uri]],
      Arbitrary.arbitrary[Option[Method]]
    ) { (headers: Headers, status: Status, errorBody: String, uri: Option[Uri], method: Option[Method]) =>
      val errorBodyBytes: ByteVector = ByteVector.view(errorBody.getBytes(StandardCharsets.UTF_8))
      val maxBodySize: Long          = errorBodyBytes.size - 1L
      val client: Client[SyncIO] = Client[SyncIO](
        Function.const(
          Resource.pure[SyncIO, Response[SyncIO]](
            Response(
              status = status,
              headers = headers,
              body = Stream.chunk(Chunk.bytes(errorBodyBytes.toArray)).covary
            )
          )
        )
      )
      val expectedBody: ByteVector =
        if (errorBodyBytes.size <= 1) {
          ByteVector.empty
        } else {
          errorBodyBytes.reverse.drop(1L).reverse
        }

      client
        .expectOptionOr[String](testRequest)(ResponseError.fromResponse_(maxBodySize, uri, method))
        .attempt
        .map(
          _.fold(
            e =>
              e match {
                case e: ResponseError =>
                  ((e.status ?= status) :| "Expected Status") &&
                    ((e.headers ?= headers) :| "Expected Headers") &&
                    ((e.requestUri ?= uri) :| "Expected URI") &&
                    ((e.requestMethod ?= method) :| "Expected Method") &&
                    ((e.errorBody ?= ResponseError.ErrorBody.PartialErrorBody(expectedBody)) :| "Expected ErrorBody") &&
                    ((e.maxBodySize ?= maxBodySize) :| "Expected maxBodySize")
                case _ =>
                  exception(e)
              },
            r => (falsified :| s"Got successful response, should have got ResponseError: ${r}")
          )
        )
        .unsafeRunSync()
    }

  property("EmptyErrorBody") =
    forAllNoShrink(
      Arbitrary.arbitrary[Headers],
      invalidStatusGen,
      Arbitrary.arbitrary[Option[Uri]],
      Arbitrary.arbitrary[Option[Method]]
    ) { (headers: Headers, status: Status, uri: Option[Uri], method: Option[Method]) =>
      val errorBodyBytes: ByteVector = ByteVector.empty
      val client: Client[SyncIO] = Client[SyncIO](
        Function.const(
          Resource.pure[SyncIO, Response[SyncIO]](
            Response(
              status = status,
              headers = headers,
              body = Stream.chunk(Chunk.bytes(errorBodyBytes.toArray)).covary
            )
          )
        )
      )

      client
        .expectOptionOr[String](testRequest)(ResponseError.fromResponse(uri, method))
        .attempt
        .map(
          _.fold(
            e =>
              e match {
                case e: ResponseError =>
                  ((e.status ?= status) :| "Expected Status") &&
                    ((e.headers ?= headers) :| "Expected Headers") &&
                    ((e.requestUri ?= uri) :| "Expected URI") &&
                    ((e.requestMethod ?= method) :| "Expected Method") &&
                    ((e.errorBody ?= ResponseError.ErrorBody.EmptyErrorBody) :| "Expected ErrorBody") &&
                    ((e.maxBodySize ?= ResponseError.ErrorBody.defaultMaxBodySizeBytes) :| "Expected maxBodySize")
                case _ =>
                  exception(e)
              },
            r => (falsified :| s"Got successful response, should have got ResponseError: ${r}")
          )
        )
        .unsafeRunSync()
    }
}
