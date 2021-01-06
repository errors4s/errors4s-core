package io.isomarcte.errors4s.core

import scala.compiletime.error
import scala.language.`3.1`
import scala.quoted._

type NEString = NEString.opaques.NEString

object NEString {

  object opaques {
    opaque type NEString = String

    private[NEString] def unsafeFrom(value: String): NEString =
      value

    extension (neString: NEString) {
      def value: String = neString
    }
  }

  private inline def isEmpty(inline value: String): Boolean =
    ${ isEmptyImpl('value) }

  private def isEmptyImpl(expr: Expr[String])(using Quotes): Expr[Boolean] =
    expr.valueOrError match {
      case "" => '{true}
      case _ => '{false}
    }

  inline def apply(inline value: String): NEString = {
    inline if(isEmpty(value)) {
      error("String is empty")
    } else {
      opaques.unsafeFrom(value)
    }
  }

  inline def from(inline value: String): Either[String, NEString] =
    if (value.nonEmpty) {
      Right(opaques.unsafeFrom(value))
    } else {
      Left("String is empty")
    }

  inline def unapply(inline value: String): Option[NEString] =
    from(value).toOption

  inline def unsafeFrom(inline value: String): NEString =
    opaques.unsafeFrom(value)
}
