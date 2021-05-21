package org.errors4s.http

import cats.data._
import cats.syntax.all._
import io.circe._

package object circe {

  private[circe] lazy val restrictedKeys: NonEmptySet[String] = NonEmptySet
    .of("type", "title", "status", "detail", "instance")

  private[circe] def filterRestrictedKeys(value: JsonObject): JsonObject =
    value.filterKeys(key => restrictedKeys.contains(key) === false)
}
