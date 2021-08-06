package org.errors4s.core.cats

import cats.kernel.laws.discipline._
import munit._
import org.errors4s.core._
import org.errors4s.core.cats.instances._
import org.errors4s.core.scalacheck.instances._

final class NonEmptyStringLawTests extends DisciplineSuite {
  checkAll("NonEmptyString.HashLaws", HashTests[NonEmptyString].hash)
  checkAll("NonEmptyString.OrderLaws", OrderTests[NonEmptyString].order)
}
