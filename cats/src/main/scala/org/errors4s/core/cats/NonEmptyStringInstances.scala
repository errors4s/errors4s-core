package org.errors4s.core.cats

import _root_.cats.Show
import _root_.cats.kernel.Hash
import _root_.cats.kernel.Order
import _root_.cats.kernel.Semigroup
import org.errors4s.core._

private[cats] trait NonEmptyStringInstances {

  implicit final val catsOrderAndHashForNonEmptyString: Hash[NonEmptyString] with Order[NonEmptyString] =
    new Hash[NonEmptyString] with Order[NonEmptyString] {
      override def hash(a: NonEmptyString): Int = a.hashCode

      override def compare(a: NonEmptyString, b: NonEmptyString): Int = Order[String].compare(a.value, b.value)
    }

  implicit final val catsInstancesForShow: Show[NonEmptyString] = Show.fromToString[NonEmptyString]

  implicit final val semigroupInstanceForNonEmptyString: Semigroup[NonEmptyString] = Semigroup
    .instance[NonEmptyString]((a, b) => NonEmptyString.concat(a, b))
}
