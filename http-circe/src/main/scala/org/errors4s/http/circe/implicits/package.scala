package org.errors4s.http.circe

package object implicits
    extends implicits.DefaultHttpErrorCodec
    with implicits.DefaultHttpProblemCodec
    with implicits.HttpStatusInstances
