package io.isomarcte.errors4s.http

import scala.reflect.macros.blackbox.Context

sealed trait HttpStatus {
  def value: Int

  // final //

  final override def toString: String = s"HttpStatus(value = ${value})"
}

object HttpStatus {
  final private[this] case class HttpStatusImpl(override val value: Int) extends HttpStatus

  def from(value: Int): Either[String, HttpStatus] =
    if (value >= 100 && value < 600) {
      Right(HttpStatusImpl(value))
    } else {
      Left(s"Invalid HTTP status code: ${value}")
    }

  def apply(value: Int): HttpStatus = macro make

  def make(c: Context)(value: c.Expr[Int]): c.Expr[HttpStatus] = {
    import c.universe._
    value.tree match {
      case Literal(Constant(value: Int)) =>
        from(value).fold(
          error => c.abort(c.enclosingPosition, error),
          value =>
            c.Expr(
              q"""_root_.io.isomarcte.errors4s.http.HttpStatus.from(${value
                .value}).toOption.getOrElse(throw new AssertionError("Error during macro expansion of HttpStatus. This is a bug in errors4s-http. Please report it."))"""
            )
        )
      case _ =>
        c.abort(c.enclosingPosition, s"HttpStatus.apply can only be used with a Int literal >= 100 and < 600.")
    }
  }
}
