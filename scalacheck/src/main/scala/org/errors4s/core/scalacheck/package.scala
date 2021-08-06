package org.errors4s.core

package object scalacheck {

  /** Scalacheck instances for errors4s core types, e.g. [[org.errors4s.core.NonEmptyString]].
    */
  object instances extends NonEmptyStringInstances
}
