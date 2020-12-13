package io.isomarcte.errors4s.http

import io.isomarcte.errors4s.core._

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
  * @see [[https://tools.ietf.org/html/rfc7807]]
  */
trait HttpError extends Error {

  /** The type of the error. Per RFC 7807 this SHOULD be a URI.
    *
    * @see [[https://tools.ietf.org/html/rfc7807#section-3.1]]
    */
  def `type`: NEString

  /** The title of the error. Per RFC 7807 this should be a short human readable
    * summary of the problem. It should not change from occurrence to
    * occurrence of the problem, e.g. it should have a functional dependency
    * on `type`.
    *
    * @see [[https://tools.ietf.org/html/rfc7807#section-3.1]]
    */
  def title: NEString

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
  def instance: Option[NEString]

  final override lazy val primaryErrorMessage: NEString = detail
    .flatMap(value => NEString.from(value).toOption)
    .getOrElse(title)
}

object HttpError {

  /** The trivial implementation of [[HttpError]]. */
  final case class SimpleHttpError(
    override val `type`: NEString,
    override val title: NEString,
    override val status: HttpStatus,
    override val detail: Option[String],
    override val instance: Option[NEString]
  ) extends HttpError
}
