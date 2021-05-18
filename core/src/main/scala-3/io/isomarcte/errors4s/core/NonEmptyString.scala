package io.isomarcte.errors4s.core

import scala.language.future
import scala.quoted.*
import scala.compiletime.*

type NonEmptyString = NonEmptyString.opaques.NonEmptyString

object NonEmptyString {

  object opaques {
    opaque type NonEmptyString = String

    def unsafe(value: String): NonEmptyString =
      value

    extension (nes: NonEmptyString) {
      def value: String = nes
      def ++(that: String): NonEmptyString = nes.value ++ that
    }
  }

  private inline def isEmpty(inline value: String): Boolean =
    ${ isEmptyImpl('value) }

  private def isEmptyImpl(expr: Expr[String])(using Quotes): Expr[Boolean] = {
    import quotes.reflect.report
    expr.value.fold(
      report.throwError("String value is not a literal constant. If at least part of the String is a literal constant, you can use the `nes` String interpolator. You can access it by import io.isomarcte.errors4s.core.syntax.all.*")
    ){
      case "" => Expr(true)
      case _ => Expr(false)
    }
  }

  inline def from(inline value: String): Either[String, NonEmptyString] =
    if (value.size > 0) {
      Right(opaques.unsafe(value))
    } else {
      Left("Unable to create NonEmptyString from empty string value.")
    }

  inline def apply(inline value: String): NonEmptyString =
    inline if (isEmpty(value)) {
      error("Unable to create NonEmptyString from empty string value.")
    } else {
      opaques.unsafe(value)
    }

  inline def concat(head: NonEmptyString, tail: String): NonEmptyString =
    head ++ tail

  private[core] inline def unsafe(inline value: String): NonEmptyString =
    opaques.unsafe(value)
}
