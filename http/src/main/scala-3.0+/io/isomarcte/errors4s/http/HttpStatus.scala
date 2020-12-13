package io.isomarcte.errors4s.http

import scala.compiletime.error
import scala.language.`3.1`
import scala.quoted._

type HttpStatus = HttpStatus.opaques.HttpStatus

object HttpStatus {
  object opaques {
    opaque type HttpStatus = Int

    def unsafeFrom(value: Int): HttpStatus =
      value

    def from(value: Int): Either[String, HttpStatus] =
      if (value < 100 || value >= 600) {
        Left("Valid HTTP status codes are >= 100 and < 599. Found: $value")
      } else {
        Right(value)
      }

    def unapply(value: Int): Option[HttpStatus] =
      from(value).toOption

    extension (httpStatus: HttpStatus) {
      def value: Int = httpStatus
    }
  }

  inline def apply(inline value: Int): HttpStatus =
    inline if (value < 100 || value >= 600) {
      error("Valid HTTP status codes are >= 100 and < 599.")
    } else {
      opaques.unsafeFrom(value)
    }

  export opaques.unsafeFrom
  export opaques.from
  export opaques.unapply
}
