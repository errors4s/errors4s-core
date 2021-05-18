package io.isomarcte.errors4s.core

import scala.reflect.macros.blackbox.Context

sealed trait NonEmptyString {
  def value: String

  // final //

  final def ++(value: String): NonEmptyString = NonEmptyString.concat(this, value)

  final def ++(value: NonEmptyString): NonEmptyString = NonEmptyString.concatNes(this, value)

  final override def toString: String = s"NonEmptyString(value = $value)"
}

object NonEmptyString {

  final private[this] case class NonEmptyStringImpl(override val value: String) extends NonEmptyString

  def from(value: String): Either[String, NonEmptyString] =
    Option(value).fold(
      Left("Given String value was null. This is not permitted for NonEmptyString values."): Either[
        String,
        NonEmptyString
      ]
    )(value =>
      value.size match {
        case size if size <= 0 =>
          Left("Given String was \"\". This is not permitted for NonEmptyString values.")
        case _ =>
          Right(NonEmptyStringImpl(value))
      }
    )

  def concat(head: NonEmptyString, tail: String): NonEmptyString = NonEmptyStringImpl(head.value ++ tail)

  def concatNes(head: NonEmptyString, tail: NonEmptyString): NonEmptyString = concat(head, tail.value)

  def apply(value: String): NonEmptyString = macro make

  def make(c: Context)(value: c.Expr[String]): c.Expr[NonEmptyString] = {
    import c.universe._
    value.tree match {
      case Literal(Constant(value: String)) if value.size > 0 =>
        c.Expr(
          q"""_root_.io.isomarcte.errors4s.core.NonEmptyString.from($value).getOrElse(throw new AssertionError("Error during macro expansion of NonEmptyString. This is a bug in errors4s-core. Please report it."))"""
        )
      case _ =>
        c.abort(
          c.enclosingPosition,
          "NonEmptyString.apply can only be used with non-empty String literal values. Consider using the `nes` interpolator which is more flexible. You can access by importing io.isomarcte.errors4s.core.syntax.all._"
        )
    }
  }

  private[core] def unsafe(value: String): NonEmptyString =
    from(value).fold(error => throw new IllegalArgumentException(error), identity)
}
