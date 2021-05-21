package org.errors4s.http

import org.errors4s.core._

/** An error type which represents an HTTP related problem.
  *
  * This is an implementation of RFC 7807. In order to encourage the
  * generation of more useful error values, this type has stricter
  * requirements on the fields than is required by the RFC. For an
  * implementation which maps exactly to the requirements of RFC 7807 see
  * [[HttpProblem]]. Usage of [[HttpProblem]] is discouraged when generating
  * errors. It should only be used when you need to parse an error, and even
  * then only as a fallback.
  *
  * @note RFC 7807 allows for the specification of arbitrary fields in
  *       addition to the standard ones. Unfortunately, there isn't a
  *       convenient to represent that in [[HttpError]] itself, without making
  *       assumptions about the serialization format, e.g. JSON, XML,
  *       etc. Implementations which require these extension fields are
  *       encouraged to extend [[HttpError]] to add them.
  *
  * @see [[https://tools.ietf.org/html/rfc7807]]
  */
trait HttpError extends Error {

  /** The type of the error. Per RFC 7807 this SHOULD be a URI.
    *
    * @see [[https://tools.ietf.org/html/rfc7807#section-3.1]]
    */
  def `type`: NonEmptyString

  /** The title of the error. Per RFC 7807 this should be a short human readable
    * summary of the problem. It should not change from occurrence to
    * occurrence of the problem, e.g. it should have a functional dependency
    * on `type`.
    *
    * @see [[https://tools.ietf.org/html/rfc7807#section-3.1]]
    */
  def title: NonEmptyString

  /** The status code associated with the problem. It SHOULD map to status codes
    * for HTTP (RFC 7231).
    *
    * @see [[https://tools.ietf.org/html/rfc7807#section-3.1]]
    * @see [[https://tools.ietf.org/html/rfc7231]]
    */
  def status: HttpStatus

  /** Detail information about the error. Unlike [[#title]], this may change
    * from occurrence to occurrence of the problem.
    *
    * @see [[https://tools.ietf.org/html/rfc7807#section-3.1]]
    */
  def detail: Option[String]

  /** This SHOULD be a URI reference unique to this instance of the problem.
    *
    * @see [[https://tools.ietf.org/html/rfc7807#section-3.1]]
    */
  def instance: Option[NonEmptyString]

  final override lazy val primaryErrorMessage: NonEmptyString = detail
    .flatMap(value => NonEmptyString.from(value).toOption)
    .getOrElse(title)
}

object HttpError {

  /** The trivial implementation of [[HttpError]]. */
  final private[this] case class HttpErrorImpl(
    override val `type`: NonEmptyString,
    override val title: NonEmptyString,
    override val status: HttpStatus,
    override val detail: Option[String],
    override val instance: Option[NonEmptyString]
  ) extends HttpError {
    final override lazy val toString: String =
      s"HttpError(type = ${`type`}, title = ${title}, status = ${status}, detail = ${detail}, instance = ${instance})"
  }

  /** The trivial [[HttpError]] implementation. */
  def simple(
    `type`: NonEmptyString,
    title: NonEmptyString,
    status: HttpStatus,
    detail: Option[String],
    instance: Option[NonEmptyString]
  ): HttpError = HttpErrorImpl(`type`, title, status, detail, instance)
}
