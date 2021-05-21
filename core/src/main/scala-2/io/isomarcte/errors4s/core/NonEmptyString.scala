package io.isomarcte.errors4s.core

import scala.reflect.macros.blackbox.Context

/** A newtype for a non empty string value. */
sealed trait NonEmptyString {
  def value: String

  // final //

  /** Concatenate a [[java.lang.String]] value with this [[NonEmptyString]]
    * value.
    */
  final def ++(value: String): NonEmptyString = NonEmptyString.concat(this, value)

  final override def toString: String = s"$value"
}

object NonEmptyString {

  final private[this] case class NonEmptyStringImpl(override val value: String) extends NonEmptyString

  /** Create a [[NonEmptyString]] value from a compile time literal
    * [[java.lang.String]] value which is non-empty. This will fail to compile
    * if the value is not a literal or is an empty literal, e.g. `""`.
    *
    * @note This method only supports a compile time literal
    *       [[java.lang.String]]. You can't interpolate into it. A more
    *       powerful mechanism for creating a compile time [[NonEmptyString]]
    *       value is the `nes` interpolator which can create a value and
    *       interpolate arbitrary values into it as well, as long as at least
    *       on component of the StringContext is a compile time non-empty
    *       String literal.
    *
    * {{{
    * scala> import io.isomarcte.errors4s.core.syntax.all._
    * import io.isomarcte.errors4s.core.syntax.all._
    *
    * scala> nes"""A non empty string \${Some("with interpolation")}"""
    *
    * val res0: io.isomarcte.errors4s.core.NonEmptyString = A non empty string Some(with interpolation)
    *
    * scala>
    * }}}
    */
  def apply(value: String): NonEmptyString = macro make

  /** Attempt to create a [[NonEmptyString]] from a [[java.lang.String]]
    * value. This will fail if the value is empty.
    */
  def from(value: String): Either[String, NonEmptyString] =
    Option(value).fold(
      Left("Given String value was null. This is not permitted for NonEmptyString values."): Either[
        String,
        NonEmptyString
      ]
    )(value =>
      value.size match {
        case size if size <= 0 =>
          Left("Unable to create NonEmptyString from empty string value.")
        case _ =>
          Right(NonEmptyStringImpl(value))
      }
    )

  /** Create a [[NonEmptyString]] value from an arbitrary [[java.lang.String]]
    * value. This will throw an exception if the value is empty.
    *
    * @note Use of this method for anything other than test code or the REPL
    *       is ''strongly'' discouraged.
    */
  def unsafe(value: String): NonEmptyString =
    from(value).fold(error => throw new IllegalArgumentException(error), identity)

  /** Concatenate a [[NonEmptyString]] and a [[java.lang.String]] value. */
  def concat(head: NonEmptyString, tail: String): NonEmptyString = NonEmptyStringImpl(head.value ++ tail)

  /** Concatenate two [[NonEmptyString]] values. */
  def concatNes(head: NonEmptyString, tail: NonEmptyString): NonEmptyString = concat(head, tail.value)

  /** Macro to generate a [[NonEmptyString]] value from a compile time literal
    * [[java.lang.String]] value which is non-empty.
    */
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
}
