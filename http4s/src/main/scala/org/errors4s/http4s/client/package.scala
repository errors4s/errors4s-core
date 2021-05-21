package org.errors4s.http4s

package object client {
  type ClientResponseErrorNoBody   = ClientResponseError[Nothing]
  type ClientResponseErrorTextBody = ClientResponseError[String]
}
