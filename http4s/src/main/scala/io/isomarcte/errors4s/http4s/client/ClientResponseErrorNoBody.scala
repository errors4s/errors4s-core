package io.isomarcte.errors4s.http4s.client

import io.isomarcte.errors4s.http4s._
import org.http4s._

object ClientResponseErrorNoBody {
  def fromOptionRequestResponseWithConfig[F[_]](
    config: RedactionConfiguration
  )(request: Option[Request[F]])(response: Response[F]): ClientResponseErrorNoBody =
    ClientResponseError.fromOptionRequestResponseWithConfig[F, Nothing](config)(request)(response)

  def fromRequestResponseWithConfig[F[_]](config: RedactionConfiguration)(request: Request[F])(
    response: Response[F]
  ): ClientResponseErrorNoBody = fromOptionRequestResponseWithConfig[F](config)(Some(request))(response)

  def fromRequestResponse[F[_]](request: Request[F])(response: Response[F]): ClientResponseErrorNoBody =
    fromRequestResponseWithConfig[F](RedactionConfiguration.default)(request)(response)

  def fromResponseWithConfig[F[_]](config: RedactionConfiguration)(response: Response[F]): ClientResponseErrorNoBody =
    fromOptionRequestResponseWithConfig[F](config)(None)(response)

  def fromResponse[F[_]](response: Response[F]): ClientResponseErrorNoBody =
    fromResponseWithConfig[F](RedactionConfiguration.default)(response)
}
