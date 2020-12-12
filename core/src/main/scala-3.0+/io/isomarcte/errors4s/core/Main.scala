package io.isomarcte.errors4s.core

object Main {
  def main(args: Array[String]): Unit = {
    val neString: NEString = NEString("foo")
    val s: String = neString.value
  }
}
