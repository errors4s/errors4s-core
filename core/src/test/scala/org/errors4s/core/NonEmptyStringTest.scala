package org.errors4s.core

import munit._
import org.errors4s.core.syntax.all._

final class NonEmptyStringTest extends FunSuite {

  test("nes interpolator") {
    assertEquals(nes" ", NonEmptyString.unsafe(" "))
    assertEquals(nes"a ${1 + 2}", NonEmptyString.unsafe("a 3"))
    assertEquals(nes"${1 + 2} a", NonEmptyString.unsafe("3 a"))
    assertEquals(nes"${1 + 2} a ${2 - 1}", NonEmptyString.unsafe("3 a 1"))
  }
}
