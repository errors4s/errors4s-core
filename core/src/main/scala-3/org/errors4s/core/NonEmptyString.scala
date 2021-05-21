package org.errors4s.core

import scala.language.future
import scala.quoted.*
import scala.compiletime.*

/** A newtype for a non empty string value. */
type NonEmptyString = NonEmptyString.opaques.NonEmptyString

object NonEmptyString {

  object opaques {
    opaque type NonEmptyString = String

  /** Create a [[NonEmptyString]] value from an arbitrary [[java.lang.String]]
    * value. This will throw an exception if the value is empty.
    *
    * @note Use of this method for anything other than test code or the REPL
    *       is ''strongly'' discouraged.
    */
    def unsafe(value: String): NonEmptyString =
      if (value.size <= 0) {
        throw new IllegalArgumentException("Can not create NonEmptyString from empty String.")
      } else {
        value
      }

    def value(nes: NonEmptyString): String =
      nes
  }

  export opaques.unsafe

  extension (inline nes: NonEmptyString) {
    inline def value: String = opaques.value(nes)
    /** Concatenate a [[java.lang.String]] value with this [[NonEmptyString]]
      * value.
      */
    inline def ++(inline that: String): NonEmptyString = concat(nes, that)
  }

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
    * scala> import org.errors4s.core.syntax.all._
    * import org.errors4s.core.syntax.all._
    *
    * scala> nes"""A non empty string ${Some("with interpolation")}"""
    *
    * val res0: org.errors4s.core.NonEmptyString = A non empty string Some(with interpolation)
    *
    * scala>
    * }}}
    */
  inline def apply(inline value: String): NonEmptyString =
    inline if (isEmpty(value)) {
      error("Unable to create NonEmptyString from empty string value.")
    } else {
      unsafe(value)
    }

  /** Attempt to create a [[NonEmptyString]] from a [[java.lang.String]]
    * value. This will fail if the value is empty.
    */
  inline def from(inline value: String): Either[String, NonEmptyString] =
    if (value.size > 0) {
      Right(unsafe(value))
    } else {
      Left("Unable to create NonEmptyString from empty string value.")
    }

  /** Concatenate a [[NonEmptyString]] and a [[java.lang.String]] value. */
  inline def concat(inline nes: NonEmptyString, inline that: String): NonEmptyString =
    unsafe(nes.value ++ that)

  /** Concatenate two [[NonEmptyString]] values. */
  inline def concatNes(inline value: NonEmptyString, inline that: NonEmptyString) =
    concat(value, that.value)

  private inline def isEmpty(inline value: String): Boolean =
    ${ isEmptyImpl('value) }

  private def isEmptyImpl(expr: Expr[String])(using Quotes): Expr[Boolean] = {
    import quotes.reflect.report
    expr.value.fold(
      report.throwError("String value is not a literal constant. If at least part of the String is a literal constant, you can use the `nes` String interpolator. You can access it by import org.errors4s.core.syntax.all.*")
    ){
      case "" => Expr(true)
      case _ => Expr(false)
    }
  }
}
