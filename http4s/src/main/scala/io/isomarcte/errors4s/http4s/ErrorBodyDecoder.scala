package io.isomarcte.errors4s.http4s.client

import cats._
import cats.effect._
import cats.syntax.all._
import org.http4s._

sealed trait ErrorBodyDecoder[F[_], A] {
  def entityDecoder: EntityDecoder[F, A]
  def showErrorBody(a: A): Option[String]
}

object ErrorBodyDecoder {

  def fromEntityDecoder[F[_], A: Show](decoder: EntityDecoder[F, A]): ErrorBodyDecoder[F, A] =
    new ErrorBodyDecoder[F, A] {
      override val entityDecoder: EntityDecoder[F, A]  = decoder
      override def showErrorBody(a: A): Option[String] = Some(a.show)
    }

  def textErrorBodyDecoderWithLengthAndCharset[F[_]: Sync](
    maxLength: Option[Long],
    charset: java.nio.charset.Charset
  ): ErrorBodyDecoder[F, String] = {
    val textDecoder: EntityDecoder[F, String] = EntityDecoder.text[F](Sync[F], Charset.fromNioCharset(charset))
    new ErrorBodyDecoder[F, String] {
      override val entityDecoder: EntityDecoder[F, String] =
        new EntityDecoder[F, String] {
          override def decode(m: Media[F], strict: Boolean): DecodeResult[F, String] =
            maxLength.fold(textDecoder.decode(m, strict))(maxLength =>
              textDecoder.decode(Media(m.body.take(maxLength), m.headers), strict)
            )

          override def consumes: Set[MediaRange] = textDecoder.consumes
        }
      override def showErrorBody(a: String): Option[String] = Some(a)
    }
  }

  def textErrorBodyDecoderUTF8[F[_]: Sync]: ErrorBodyDecoder[F, String] =
    textErrorBodyDecoderWithLengthAndCharset[F](None, java.nio.charset.StandardCharsets.UTF_8)
}
