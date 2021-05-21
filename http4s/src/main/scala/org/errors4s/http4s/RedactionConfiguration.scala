package org.errors4s.http4s

import cats.syntax.all._
import org.errors4s.http4s.headers._
import org.http4s._
import scala.annotation.nowarn

sealed trait RedactionConfiguration {
  import RedactionConfiguration._

  def redactRequestHeaderValue: RedactRequestHeaderValue
  def redactResponseHeaderValue: RedactResponseHeaderValue
  def redactUriQueryParamValue: RedactUriQueryParam

  // final //

  final def withRedactRequestHeaderValue(f: RedactRequestHeaderValue): RedactionConfiguration =
    RedactionConfiguration.withRedactRequestHeaderValue(this)(f)

  final def withRedactResponseHeaderValue(f: RedactResponseHeaderValue): RedactionConfiguration =
    RedactionConfiguration.withRedactResponseHeaderValue(this)(f)

  final def withRedactUriQueryParam(f: RedactUriQueryParam): RedactionConfiguration =
    RedactionConfiguration.withRedactUriQueryParam(this)(f)
}

object RedactionConfiguration {

  final private[this] case class RedactionConfigurationImpl(
    override val redactRequestHeaderValue: RedactRequestHeaderValue,
    override val redactResponseHeaderValue: RedactResponseHeaderValue,
    override val redactUriQueryParamValue: RedactUriQueryParam
  ) extends RedactionConfiguration

  lazy val default: RedactionConfiguration = RedactionConfigurationImpl(
    RedactRequestHeaderValue.default,
    RedactResponseHeaderValue.default,
    RedactUriQueryParam.default
  )

  lazy val unredacted: RedactionConfiguration = RedactionConfigurationImpl(
    RedactRequestHeaderValue.unredacted,
    RedactResponseHeaderValue.unredacted,
    RedactUriQueryParam.unredacted
  )

  def withRedactRequestHeaderValue(value: RedactionConfiguration)(f: RedactRequestHeaderValue): RedactionConfiguration =
    value match {
      case value: RedactionConfigurationImpl =>
        value.copy(redactRequestHeaderValue = f)
    }

  def withRedactResponseHeaderValue(
    value: RedactionConfiguration
  )(f: RedactResponseHeaderValue): RedactionConfiguration =
    value match {
      case value: RedactionConfigurationImpl =>
        value.copy(redactResponseHeaderValue = f)
    }

  def withRedactUriQueryParam(value: RedactionConfiguration)(f: RedactUriQueryParam): RedactionConfiguration =
    value match {
      case value: RedactionConfigurationImpl =>
        value.copy(redactUriQueryParamValue = f)
    }

  // Newtype related private functions and values //

  private[this] def headerInAllowedHeaders(value: Header): Boolean =
    AllowedHeaders.defaultAllowHeaders.contains(value.name)

  // Newtypes, because a lot of these functions are of the same type.

  final case class RedactRequestHeaderValue(value: Header => Header) extends AnyVal

  object RedactRequestHeaderValue {
    lazy val default: RedactRequestHeaderValue = RedactRequestHeaderValue(value =>
      if (headerInAllowedHeaders(value)) {
        value
      } else {
        Header.Raw(value.name, defaultRedactValue(value.value))
      }
    )

    lazy val unredacted: RedactRequestHeaderValue = RedactRequestHeaderValue(identity)
  }

  final case class RedactResponseHeaderValue(value: Header => Header) extends AnyVal

  object RedactResponseHeaderValue {
    lazy val default: RedactResponseHeaderValue = RedactResponseHeaderValue(value =>
      if (headerInAllowedHeaders(value)) {
        value
      } else {
        Header.Raw(value.name, defaultRedactValue(value.value))
      }
    )

    lazy val unredacted: RedactResponseHeaderValue = RedactResponseHeaderValue(identity)
  }

  final case class RedactUriQueryParam(value: (String, Option[String]) => (String, Option[String])) extends AnyVal

  object RedactUriQueryParam {

    lazy val default: RedactUriQueryParam = RedactUriQueryParam {
      case (key, Some(value)) =>
        (key, Some(redactWithConstantString("REDACTED")(value)))
      case (key, None) =>
        (key, None)
    }

    lazy val unredacted: RedactUriQueryParam = RedactUriQueryParam((key, value) => (key, value))
  }

  // Redacted newtypes, these are the results of applying the Redact newtype functions

  sealed trait RedactedRequestHeaders {
    def value: Headers

    def unredacted: Headers

    // final //

    final override def toString: String = s"RedactedRequestHeaders(value = ${value})"
  }

  object RedactedRequestHeaders {
    final private[this] case class RedactedRequestHeadersImpl(
      override val unredacted: Headers,
      redactionF: RedactRequestHeaderValue
    ) extends RedactedRequestHeaders {
      override def value: Headers = unredacted.foldMap(header => Headers.of(redactionF.value(header)))
    }

    def fromHeaders(headers: Headers, redact: RedactRequestHeaderValue): RedactedRequestHeaders =
      RedactedRequestHeadersImpl(headers, redact)

    def fromRequest[F[_]](request: Request[F], redact: RedactRequestHeaderValue): RedactedRequestHeaders =
      fromHeaders(request.headers, redact)

    def fromHeadersAndConfig(headers: Headers, config: RedactionConfiguration): RedactedRequestHeaders =
      fromHeaders(headers, config.redactRequestHeaderValue)

    def fromRequestAndConfig[F[_]](request: Request[F], config: RedactionConfiguration): RedactedRequestHeaders =
      fromHeadersAndConfig(request.headers, config)
  }

  sealed trait RedactedResponseHeaders {
    def value: Headers

    def unredacted: Headers

    // final //

    final override def toString: String = s"RedactedResponseHeaders(value = ${value})"
  }

  object RedactedResponseHeaders {
    final private[this] case class RedactedResponseHeadersImpl(
      override val unredacted: Headers,
      redactionF: RedactResponseHeaderValue
    ) extends RedactedResponseHeaders {
      override def value: Headers = unredacted.foldMap(header => Headers.of(redactionF.value(header)))
    }

    def fromHeaders(headers: Headers, redact: RedactResponseHeaderValue): RedactedResponseHeaders =
      RedactedResponseHeadersImpl(headers, redact)

    def fromResponse[F[_]](response: Response[F], redact: RedactResponseHeaderValue): RedactedResponseHeaders =
      fromHeaders(response.headers, redact)

    def fromHeadersAndConfig(headers: Headers, config: RedactionConfiguration): RedactedResponseHeaders =
      fromHeaders(headers, config.redactResponseHeaderValue)

    def fromResponseAndConfig[F[_]](response: Response[F], config: RedactionConfiguration): RedactedResponseHeaders =
      fromHeaders(response.headers, config.redactResponseHeaderValue)
  }

  sealed trait RedactedUri {
    def value: Uri

    def unredacted: Uri

    // final //

    final override def toString: String = s"RedcatedUri(value = ${value})"
  }

  object RedactedUri {
    final private[this] case class RedactedUriImpl(override val unredacted: Uri, redactionF: RedactUriQueryParam)
        extends RedactedUri {
      override def value: Uri =
        unredacted.copy(query =
          Query.fromVector(
            unredacted
              .query
              .pairs
              .foldMap { case (key, value) =>
                Vector(redactionF.value(key, value))
              }
          )
        )
    }

    def fromUri(uri: Uri, redact: RedactUriQueryParam): RedactedUri = RedactedUriImpl(uri, redact)

    def fromRequest[F[_]](request: Request[F], redact: RedactUriQueryParam): RedactedUri = fromUri(request.uri, redact)

    def fromUriAndConfig(uri: Uri, config: RedactionConfiguration): RedactedUri =
      fromUri(uri, config.redactUriQueryParamValue)

    def fromRequestAndConfig[F[_]](request: Request[F], config: RedactionConfiguration): RedactedUri =
      fromUri(request.uri, config.redactUriQueryParamValue)
  }

  // General utility functions, exposed so users have an easy time modifying
  // the default behavior.

  def defaultRedactValue[A](value: A): String = redactWithConstantString[A]("<REDACTED>")(value)

  def redactWithConstantString[A](constant: String)(@nowarn("cat=unused") value: A): String = constant
}
