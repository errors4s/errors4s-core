package org.errors4s.core

import org.errors4s.core.syntax.all._

final class NonEmptyStringTest extends BaseTest {

  "The nes interpolator" should "yield values for non-empty strings" in {
    nes" " shouldBe NonEmptyString.unsafe(" ")
    nes"a ${1 + 2}" shouldBe NonEmptyString.unsafe("a 3")
    nes"${1 + 2} a" shouldBe NonEmptyString.unsafe("3 a")
    nes"${1 + 2} a ${2 - 1}" shouldBe NonEmptyString.unsafe("3 a 1")
  }
}
