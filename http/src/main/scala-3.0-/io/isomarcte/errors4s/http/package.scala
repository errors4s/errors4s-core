package io.isomarcte.errors4s

import eu.timepit.refined._
import eu.timepit.refined.api._
import eu.timepit.refined.numeric._

package object http {

  /** Refined representation of valid HTTP statuses. */
  type HttpStatus = Int Refined Interval.Closed[W.`100`.T, W.`599`.T]
  object HttpStatus extends RefinedTypeOps[HttpStatus, Int]
}
