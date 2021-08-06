package org.errors4s.core.scalacheck

import org.errors4s.core._
import org.scalacheck._
import scala.annotation.implicitNotFound

private[scalacheck] trait NonEmptyStringInstances {

  @implicitNotFound("Unable to find implicit Arbitrary[String] instance. Consider importing org.scalacheck._")
  implicit final def arbNonEmptyString(implicit arbString: Arbitrary[String]): Arbitrary[NonEmptyString] = Arbitrary(
    arbString
      .arbitrary
      .map(value => NonEmptyString.from(value))
      .filter(_.isRight)
      .flatMap(_.fold(Function.const(Gen.fail), Gen.const))
  )

  @implicitNotFound("Unable to find implicit Cogen[String] instance. Consider importing org.scalacheck._")
  implicit final def cogenNonEmptyString(implicit cogenString: Cogen[String]): Cogen[NonEmptyString] = cogenString
    .contramap(_.value)
}
