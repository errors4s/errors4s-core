package io.isomarcte.errors4s

import eu.timepit.refined.api.RefinedTypeOps
import eu.timepit.refined.types.string.NonEmptyString

package object core {
  type NEString = NonEmptyString
  object NEString extends RefinedTypeOps[NonEmptyString, String]
}
