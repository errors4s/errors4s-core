package org.errors4s.http

import scala.language.future
import scala.quoted.*
import scala.compiletime.*

/** A newtype for a http status.
  *
  * This type can only represent valid HTTP status values. These are integral
  * values >= 100 and < 600.
  */
type HttpStatus = HttpStatus.opaques.HttpStatus

object HttpStatus {
  object opaques {
    opaque type HttpStatus = Int


    def value(value: HttpStatus): Int = value

  /** Create a [[HttpStatus]] value from an arbitrary integral value. This will
    * throw an exception if the value is < 100 or >= 600.
    *
    * @note Use of this method for anything other than test code or the REPL
    *       is ''strongly'' discouraged.
    */
    def unsafe(value: Int): HttpStatus =
      if (value < 100 || value >= 600) {
        throw new IllegalArgumentException(s"HttpStatus values must be >= 100 && < 600: ${value}")
      } else {
        value
      }
  }

  export opaques.unsafe

  extension (inline httpStatus: HttpStatus) {
    inline def value: Int = opaques.value(httpStatus)
  }

  /** Create a [[HttpStatus]] value from an compile time literal integral value
    * >= 100 and < 600. Only compile time literal values in the valid range
    * will compile.
    */
  inline def apply(inline value: Int): HttpStatus =
    inline if (isValid(value)) {
      unsafe(value)
    } else {
      error("Unable to create HttpStatus from literal Int, it must be >= 100 && < 600")
    }

  /** Create a [[HttpStatus]] value from an arbitrary integral value. This will
    * fail if the value is < 100 or >= 600.
    */
  inline def from(inline value: Int): Either[String, HttpStatus] =
    if (value < 100 || value >= 600) {
      Left(s"Invalid HttpStatus value, must be >= 100 && < 600: ${value}")
    } else {
      Right(unsafe(value))
    }

  /** Compile time check to see if a given [[java.lang.Int]] value is a valid
    * [[HttpStatus]].
    */
  private inline def isValid(inline value: Int): Boolean =
    ${ isValidImpl('value) }

  private def isValidImpl(expr: Expr[Int])(using Quotes): Expr[Boolean] = {
    import quotes.reflect.report
    expr.value.fold(
      report.throwError("Int value is not a compile time literal constant. HttpStatus.apply can only be used with compile time literal Int values >= 100 && < 600.")
    )((value: Int) =>
      Expr((value >= 100) && (value < 600))
    )
  }
}
