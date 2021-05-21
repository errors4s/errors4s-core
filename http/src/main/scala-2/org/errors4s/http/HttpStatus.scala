package org.errors4s.http

import scala.reflect.macros.blackbox.Context

/** A newtype for a http status.
  *
  * This type can only represent valid HTTP status values. These are integral
  * values >= 100 and < 600.
  */
sealed trait HttpStatus {

  def value: Int

  // final //

  final override def toString: String = s"${value}"
}

object HttpStatus {
  final private[this] case class HttpStatusImpl(override val value: Int) extends HttpStatus

  /** Create a [[HttpStatus]] value from an compile time literal integral value
    * >= 100 and < 600. Only compile time literal values in the valid range
    * will compile.
    */
  def apply(value: Int): HttpStatus = macro make

  /** Create a [[HttpStatus]] value from an arbitrary integral value. This will
    * fail if the value is < 100 or >= 600.
    */
  def from(value: Int): Either[String, HttpStatus] =
    if (value >= 100 && value < 600) {
      Right(HttpStatusImpl(value))
    } else {
      Left(s"Invalid HTTP status code: ${value}")
    }

  /** Create a [[HttpStatus]] value from an arbitrary integral value. This will
    * throw an exception if the value is < 100 or >= 600.
    *
    * @note Use of this method for anything other than test code or the REPL
    *       is ''strongly'' discouraged.
    */
  def unsafe(value: Int): HttpStatus = from(value).fold(error => throw new IllegalArgumentException(error), identity)

  def make(c: Context)(value: c.Expr[Int]): c.Expr[HttpStatus] = {
    import c.universe._
    value.tree match {
      case Literal(Constant(value: Int)) =>
        from(value).fold(
          error => c.abort(c.enclosingPosition, error),
          value =>
            c.Expr(
              q"""_root_.org.errors4s.http.HttpStatus.from(${value
                .value}).toOption.getOrElse(throw new AssertionError("Error during macro expansion of HttpStatus. This is a bug in errors4s-http. Please report it."))"""
            )
        )
      case _ =>
        c.abort(c.enclosingPosition, s"HttpStatus.apply can only be used with a Int literal >= 100 and < 600.")
    }
  }
}
