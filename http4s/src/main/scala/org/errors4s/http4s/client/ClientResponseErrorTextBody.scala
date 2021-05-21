package org.errors4s.http4s.client

import cats._
import cats.effect.{MonadThrow => _, _}
import org.errors4s.http4s._
import org.http4s._

object ClientResponseErrorTextBody {
  def fromOptionRequestResponseWithConfigAndDecoder[F[_]: MonadThrow](
    config: RedactionConfiguration,
    errorBodyDecoder: ErrorBodyDecoder[F, String]
  )(request: Option[Request[F]])(response: Response[F]): F[ClientResponseErrorTextBody] =
    ClientResponseError
      .fromOptionRequestResponseWithConfigWithBody[F, String](config, errorBodyDecoder)(request)(response)

  def fromOptionRequestResponseWithConfig[F[_]: Sync](
    config: RedactionConfiguration
  )(request: Option[Request[F]])(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromOptionRequestResponseWithConfigAndDecoder[F](config, ErrorBodyDecoder.textErrorBodyDecoderUTF8)(request)(
      response
    )

  def fromRequestResponseWithConfigAndDecoder[F[_]: MonadThrow](
    config: RedactionConfiguration,
    errorBodyDecoder: ErrorBodyDecoder[F, String]
  )(request: Request[F])(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromOptionRequestResponseWithConfigAndDecoder[F](config, errorBodyDecoder)(Some(request))(response)

  def fromRequestResponseWithConfig[F[_]: Sync](
    config: RedactionConfiguration
  )(request: Request[F])(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromRequestResponseWithConfigAndDecoder[F](config, ErrorBodyDecoder.textErrorBodyDecoderUTF8)(request)(response)

  def fromRequestResponseWithDecoder[F[_]: MonadThrow](
    errorBodyDecoder: ErrorBodyDecoder[F, String]
  )(request: Request[F])(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromRequestResponseWithConfigAndDecoder[F](RedactionConfiguration.default, errorBodyDecoder)(request)(response)

  def fromRequestResponse[F[_]: Sync](request: Request[F])(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromRequestResponseWithDecoder[F](ErrorBodyDecoder.textErrorBodyDecoderUTF8)(request)(response)

  def fromResponseWithConfigAndDecoder[F[_]: MonadThrow](
    config: RedactionConfiguration,
    errorBodyDecoder: ErrorBodyDecoder[F, String]
  )(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromOptionRequestResponseWithConfigAndDecoder[F](config, errorBodyDecoder)(None)(response)

  def fromResponseWithConfig[F[_]: Sync](
    config: RedactionConfiguration
  )(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromResponseWithConfigAndDecoder[F](config, ErrorBodyDecoder.textErrorBodyDecoderUTF8)(response)

  def fromResponseWithDecoder[F[_]: MonadThrow](
    errorBodyDecoder: ErrorBodyDecoder[F, String]
  )(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromResponseWithConfigAndDecoder[F](RedactionConfiguration.default, errorBodyDecoder)(response)

  def fromResponse[F[_]: Sync](response: Response[F]): F[ClientResponseErrorTextBody] =
    fromResponseWithDecoder[F](ErrorBodyDecoder.textErrorBodyDecoderUTF8)(response)
}
