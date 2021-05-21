package org.errors4s.http

import org.errors4s.core._
import org.errors4s.core.syntax.all._

/** An error type which represents an HTTP related problem.
  *
  * This is an implementation of RFC 7807. While not ''strictly required'' by
  * the RFC, instances of this type SHOULD provide a `type`, `title`, and
  * `status`.
  *
  * @note This incarnation of RFC 7807 exists to be fully compliant with the
  *       RFC spec. The RFC permits all the fields to be optional, which can
  *       lead to error values which are not very useful. There is good reason
  *       for this choice by the authors of the RFC, if you are attempting to
  *       parse an error value you don't really want to fail during parsing
  *       just because the error is ill defined, you'd like to at least get
  *       what you can from the error value. That being said, using
  *       [[HttpError]] is much preferable to this type. Users are encouraged
  *       to use [[HttpError]] instead of [[HttpProblem]].
  *
  * @see [[https://tools.ietf.org/html/rfc7807]]
  */
trait HttpProblem extends Error {

  /** The type of the error. Per RFC 7807 this SHOULD be a URI.
    *
    * @see [[https://tools.ietf.org/html/rfc7807#section-3.1]]
    */
  def `type`: Option[String]

  /** The title of the error. Per RFC 7807 this should be a short human readable
    * summary of the problem. It should not change from occurrence to
    * occurrence of the problem, e.g. it should have a functional dependency
    * on `type`.
    *
    * @see [[https://tools.ietf.org/html/rfc7807#section-3.1]]
    */
  def title: Option[String]

  /** The status code associated with the problem. It SHOULD map to status codes
    * for HTTP (RFC 7231).
    *
    * @see [[https://tools.ietf.org/html/rfc7807#section-3.1]]
    * @see [[https://tools.ietf.org/html/rfc7231]]
    */
  def status: Option[Int]

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
  def instance: Option[String]

  final override lazy val primaryErrorMessage: NonEmptyString = detail
    .orElse(title)
    .flatMap(info => NonEmptyString.from(info).toOption)
    .getOrElse(
      nes"No detail or title was provided by HttpProblem: ${Error.nameOf(this)}. It is recommended that the system generating this error correct it's encoding to provide better information about the issue per RFC 7807."
    )
}

object HttpProblem {

  /** The trivial implementation of [[HttpProblem]]. */
  final private[this] case class HttpProblemImpl(
    override val `type`: Option[String],
    override val title: Option[String],
    override val status: Option[Int],
    override val detail: Option[String],
    override val instance: Option[String]
  ) extends HttpProblem {
    final override lazy val toString: String =
      s"HttpProblem(type = ${`type`}, title = ${title}, status = ${status}, detail = ${detail}, instance = ${instance})"
  }

  /** The trivial [[HttpProblem]] implementation. */
  def simple(
    `type`: Option[String],
    title: Option[String],
    status: Option[Int],
    detail: Option[String],
    instance: Option[String]
  ): HttpProblem = HttpProblemImpl(`type`, title, status, detail, instance)
}
