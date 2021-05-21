package io.isomarcte.errors4s.core.syntax

import io.isomarcte.errors4s.core._
import scala.reflect.macros.blackbox.Context

private[core] trait NonEmptyStringSyntax {

  implicit final class NonEmptyStringContext(private val sc: StringContext) {

    /** Create a [[NonEmptyString]] from a `StringContext`. Interpolation of other
      * values is permitted as long as there is at least one non-empty string
      * literal component. This is verified at compile time.
      */
    def nes(args: Any*): NonEmptyString = macro NonEmptyStringSyntax.stringContextMacro
  }
}

private object NonEmptyStringSyntax {
  private[this] def isNonEmptyLiteralString(c: Context)(tree: c.universe.Tree): Boolean = {
    import c.universe._
    tree match {
      case Literal(Constant(value: String)) =>
        value.size > 0
      case _ =>
        false
    }
  }

  def stringContextMacro(c: Context)(args: c.Expr[Any]*): c.Expr[NonEmptyString] = {
    import c.universe._
    c.prefix.tree match {
      case Apply(_, List(Apply(_, scArgs))) if scArgs.exists(isNonEmptyLiteralString(c)) =>
        c.Expr(
          q"""io.isomarcte.errors4s.core.NonEmptyString.from(StringContext(${scArgs}: _*).s(..${args})).getOrElse(throw new AssertionError("Error during NonEmptyString macro expansion. This is an errors4s bug, please report it."))"""
        )
      case Apply(_, List(_)) =>
        c.abort(
          c.enclosingPosition,
          "Can not create NonEmptyString from StringContext unless there is at least one non-empty Literal string in the context."
        )
      case _ =>
        c.abort(
          c.enclosingPosition,
          "Invalid application of nes StringContext. This is an errors4s bug, please report it."
        )
    }
  }
}
