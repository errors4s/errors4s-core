package io.isomarcte.errors4s.http4s.circe

import cats.effect._
import org.scalatest.flatspec._
import org.scalatest.matchers.should._

abstract class BaseTest extends AnyFlatSpec with Matchers {
  def sio[A](a: SyncIO[A]): A = a.unsafeRunSync()
}
