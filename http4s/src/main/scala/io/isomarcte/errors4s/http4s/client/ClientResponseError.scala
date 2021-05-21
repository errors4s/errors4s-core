package io.isomarcte.errors4s.http4s.client

import _root_.io.isomarcte.errors4s.core._
import _root_.io.isomarcte.errors4s.core.syntax.all._
import cats._
import cats.data._
import cats.implicits._
import io.isomarcte.errors4s.http4s.RedactionConfiguration._
import io.isomarcte.errors4s.http4s._
import org.http4s._

/** Error type which can be used in the `org.http4s.client.Client.expectOr` or
  * `org.http4s.client.Client.expectOptionOr` methods of a http4s Client.
  *
  * @see [[https://http4s.org/v0.21/api/org/http4s/client/client Http4s Client]]
  */
sealed trait ClientResponseError[A] extends Error {

  def status: Status
  def requestHeaders: Option[RedactedRequestHeaders]
  def requestMethod: Option[Method]
  def requestUri: Option[RedactedUri]
  def responseHeaders: RedactedResponseHeaders
  def responseBodyT: EitherT[Option, Throwable, A]

  // protected //

  protected def showResponseBody: A => Option[String]

  // final //

  final def responseBody: Option[Either[Throwable, A]] = responseBodyT.value

  final lazy val responseBodyText: Option[String] = responseBodyT
    .foldF(Function.const(Option.empty[String]), (a: A) => showResponseBody(a))

  final lazy val requestHeadersValue: Option[Headers] = requestHeaders.map(_.value)

  final lazy val responseHeadersValue: Headers = responseHeaders.value

  final override lazy val primaryErrorMessage: NonEmptyString = requestUri
    .flatMap(_.value.host.map(host => nes"Unexpected response from HTTP call to ${host.renderString}: ${status}"))
    .getOrElse(nes"Unexpected response from HTTP call: ${status}")

  final override lazy val secondaryErrorMessages: Vector[String] =
    requestUri.map(uri => s"Request URI: ${uri.value.renderString}").toVector ++
      requestMethod.map(method => s"Request Method: ${method.renderString}").toVector ++ Vector(s"Status: ${status}") ++
      Vector(s"Response Headers: ${responseHeaders.value}") ++
      requestHeaders.map(headers => s"Request Headers: ${headers.value}").toVector ++
      responseBodyText.fold(Vector.empty[String])(value => Vector(s"Response Body: ${value.toString}"))

  final override lazy val causes: Vector[Throwable] =
    responseBodyT
      .foldF(
        (t: Throwable) =>
          Some(Error.withMessageAndCause(nes"Error occurred when attempting to decode the error response body.", t)),
        Function.const(None)
      )
      .toVector

  final override def toString: String = s"ClientResponseError(${getLocalizedMessage})"
}

object ClientResponseError {

  final private[this] case class ClientResponseErrorImpl[A](
    override val status: Status,
    override val requestHeaders: Option[RedactedRequestHeaders],
    override val requestMethod: Option[Method],
    override val requestUri: Option[RedactedUri],
    override val responseHeaders: RedactedResponseHeaders,
    override val responseBodyT: EitherT[Option, Throwable, A],
    override protected val showResponseBody: A => Option[String]
  ) extends ClientResponseError[A]

  def fromOptionRequestResponseWithConfigWithBody[F[_], A](
    config: RedactionConfiguration,
    errorBodyDecoder: ErrorBodyDecoder[F, A]
  )(
    request: Option[Request[F]]
  )(response: Response[F])(implicit F: MonadError[F, Throwable]): F[ClientResponseError[A]] = {
    val buildError: EitherT[Option, Throwable, A] => ClientResponseError[A] =
      (
        decodeResult =>
          ClientResponseErrorImpl(
            response.status,
            request.map(value => RedactedRequestHeaders.fromRequestAndConfig(value, config)),
            request.map(_.method),
            request.map(value => RedactedUri.fromRequestAndConfig(value, config)),
            RedactedResponseHeaders.fromResponseAndConfig(response, config),
            decodeResult,
            a => errorBodyDecoder.showErrorBody(a)
          )
      )

    response
      .as[A](F, errorBodyDecoder.entityDecoder)
      .redeem((t: Throwable) => buildError(EitherT.leftT(t)), (a: A) => buildError(EitherT.rightT(a)))
  }

  def fromOptionRequestResponseWithConfig[F[_], A](
    config: RedactionConfiguration
  )(request: Option[Request[F]])(response: Response[F]): ClientResponseError[A] = {
    ClientResponseErrorImpl(
      response.status,
      request.map(value => RedactedRequestHeaders.fromRequestAndConfig(value, config)),
      request.map(_.method),
      request.map(value => RedactedUri.fromRequestAndConfig(value, config)),
      RedactedResponseHeaders.fromResponseAndConfig(response, config),
      EitherT[Option, Throwable, A](None),
      Function.const(None)
    )
  }

  def fromRequestResponseWithConfig[F[_], A](config: RedactionConfiguration)(request: Request[F])(
    response: Response[F]
  ): ClientResponseError[A] = fromOptionRequestResponseWithConfig[F, A](config)(Some(request))(response)

  def fromRequestResponse[F[_], A](request: Request[F])(response: Response[F]): ClientResponseError[A] =
    fromRequestResponseWithConfig[F, A](RedactionConfiguration.default)(request)(response)

  def fromResponseWithConfig[F[_], A](config: RedactionConfiguration)(response: Response[F]): ClientResponseError[A] =
    fromOptionRequestResponseWithConfig[F, A](config)(None)(response)

  def fromResponse[F[_], A](response: Response[F]): ClientResponseError[A] =
    fromResponseWithConfig[F, A](RedactionConfiguration.default)(response)

  def fromRequestResponseWithConfigWithBody[F[_]: MonadThrow, A](
    config: RedactionConfiguration,
    errorBodyDecoder: ErrorBodyDecoder[F, A]
  )(request: Request[F])(response: Response[F]): F[ClientResponseError[A]] =
    fromOptionRequestResponseWithConfigWithBody[F, A](config, errorBodyDecoder)(Some(request))(response)

  def fromRequestResponseWithBody[F[_]: MonadThrow, A](
    errorBodyDecoder: ErrorBodyDecoder[F, A]
  )(request: Request[F])(response: Response[F]): F[ClientResponseError[A]] =
    fromRequestResponseWithConfigWithBody[F, A](RedactionConfiguration.default, errorBodyDecoder)(request)(response)

  def fromResponseWithConfigWithBody[F[_]: MonadThrow, A](
    config: RedactionConfiguration,
    errorBodyDecoder: ErrorBodyDecoder[F, A]
  )(response: Response[F]): F[ClientResponseError[A]] =
    fromOptionRequestResponseWithConfigWithBody[F, A](config, errorBodyDecoder)(None)(response)

  def fromResponseWithBody[F[_]: MonadThrow, A](
    errorBodyDecoder: ErrorBodyDecoder[F, A]
  )(response: Response[F]): F[ClientResponseError[A]] =
    fromResponseWithConfigWithBody[F, A](RedactionConfiguration.default, errorBodyDecoder)(response)
}
