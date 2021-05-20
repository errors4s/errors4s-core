package io.isomarcte.errors4s.http

final class HttpStatusTest extends BaseTest {

  "Creating compile time valid HttpStatus values" should "compile correctly" in {
    HttpStatus(100) shouldBe HttpStatus.unsafe(100)
  }
}
