# Errors For Scala #

This project has the following goals,

* Provide a better base type for errors than `Throwable`.
* Provide an implementation of [RFC 7807][rfc-7807]

# ScalaDoc #

* [core][core-scaladoc]
* [http][http-scaladoc]
* [http-circe][http-circe]
* [http4s-circe][http4s-circe]

[core-scaladoc]: https://oss.sonatype.org/service/local/repositories/releases/archive/io/isomarcte/errors4s-core_2.13/0.0.2/errors4s-core_2.13-0.0.2-javadoc.jar/!/io/isomarcte/errors4s/core/index.html "Core Scaladoc"
[http-scaladoc]: https://oss.sonatype.org/service/local/repositories/releases/archive/io/isomarcte/errors4s-http_2.13/0.0.2/errors4s-http_2.13-0.0.2-javadoc.jar/!/io/isomarcte/errors4s/http/index.html "Http Scaladoc"
[http-circe]: https://oss.sonatype.org/service/local/repositories/snapshots/archive/io/isomarcte/errors4s-http-circe_2.13/0.0.1-SNAPSHOT/errors4s-http-circe_2.13-0.0.1-SNAPSHOT-javadoc.jar/!/io/isomarcte/errors4s/http/circe/index.html "Http Circe Scaladoc"
[http4s-circe]: https://oss.sonatype.org/service/local/repositories/releases/archive/io/isomarcte/errors4s-http4s-circe_2.13/0.0.2/errors4s-http4s-circe_2.13-0.0.2-javadoc.jar/!/io/isomarcte/errors4s/http4s/circe/index.html "Http4s Circe Scaladoc"

# Overview #

This project provides a better root error type `Error` which extends `Throwable` but provides better minimum constraints on what must be declared with the error. The `core` module has one  dependencies, [refined][refined]. [refined][refined] is used to enforce the fundamental concept of `errors4s`, when throwing an error you must provide enough information to make that error _useful_.

# Why #

Before we discuss the details of this project, it might be helpful for a moment to motivate its existence.

In both OO and FP Scala code, it is common to use `Throwable` as the root type for errors. This is convenient as it allows for simple interoperability with existing JVM/Java code. `Throwable` however has several less than helpful constructions. For example,

```scala mdoc
val t: Throwable = new RuntimeException()
t.getMessage
t.getCause
```

The very last thing that you want to happen when you are debugging an error is to be given absolutely no information about. Okay, so this is clearly a contrived example, but this does happen in the real world. Here is a bit more realistic of a motivating example. Suppose we are using a library for parsing which and we are adapting the error to our a new error type.

```scala mdoc
// From some hypothetical parsing library

final class InvalidIntException extends RuntimeException

def parseInt(value: String): Int =
    try {
        value.toInt
    } catch {
        case _: Throwable => throw new InvalidIntException
    }

// In our code

import scala.util.Try

case class OurException(message: String) extends RuntimeException(message)

def add(a: String, b: String): Either[OurException, Int] =
    (for {
      c <- Try(parseInt(a))
      d <- Try(parseInt(b))
    } yield c + d).fold(
        e => Left(OurException(e.getMessage)),
        i => Right(i)
    )

add("1", "a")
```

In this example the library intends to indicate the type of the error in the class name of the thrown exception, thus it uses `null` as the message. This is a not uncommon idiom in some codebases. Unfortunately our code is adapting the error to a new exception type, another common idiom, and it is relying on the value of `getMessage` from the underlying to indicate information about the error. At the end of the day this leaves us with the useless and infuriating message `null`.

# Modules #

## Core ##

The core module defines a new type `Error`. It extends `RuntimeException` (and thus `Throwable`), but provides stronger constraints on the required information. It is an open `trait` so new domain specific error types are free to extend it (as is the common idiom with `RuntimeException`). The fundamental member of `Error` is `primaryErrorMessage`, a [refined][refined] `NonEmptyString`. For example, the following code will not compile.

```scala mdoc:fail
import eu.timepit.refined.types.all._
import io.isomarcte.errors4s.core._

Error.withMessage(null: NonEmptyString)
```

Nor will this code,

```scala
import eu.timepit.refined.types.all._
import io.isomarcte.errors4s.core._

Error.withMessage(NonEmptyString(""))
```

but this code does compile,

```scala mdoc
import eu.timepit.refined.types.all._
import io.isomarcte.errors4s.core._

val e: Error = Error.withMessage(NonEmptyString("Failure During Parsing"))
```

`primaryErrorMessage` represents the unchanging context of the error. In order to generate a `NonEmptyString` (as opposed to an `Either[String, NonEmptyString]`) at compile time you have to provide a literal `String` value, e.g. `"Failure During Parsing"`. An interpolated value will not work, e.g. `s"Parsing failure: ${value}"`. For providing more context specific information about the error you should use the `secondaryErrorMessages: List[String]` field.

Since `Error` extends `Throwable` we can interoperate with code which expects `Throwable` with no issues.

```scala mdoc
def adaptError(t: Throwable): RuntimeException =
  new RuntimeException(t.getMessage)

adaptError(e)
```

`Error` also provides a built in method to attempt to handle situations where the class name of some arbitrary `Throwable` was intended to communicate why an error occurred. Going back to our original example, we can use `Error.fromThrowable` to get a much more useful error.

```scala mdoc:reset
import io.isomarcte.errors4s.core._
import scala.util.Try

// From some hypothetical parsing library

final class InvalidIntException extends RuntimeException

def parseInt(value: String): Int =
    try {
        value.toInt
    } catch {
        case _: Throwable => throw new InvalidIntException
    }

// In our code

def add(a: String, b: String): Either[Throwable, Int] =
    (for {
      c <- Try(parseInt(a))
      d <- Try(parseInt(b))
    } yield c + d).fold(
        e => Left(Error.fromThrowable(e)),
        i => Right(i)
    )

add("1", "a")
```

You can see that since the `InvalidIntException` didn't have a defined `getMessage` `Error.fromThrowable` did inspection on the class name as a fallback method of generating an error message. Note, `errors4s` does _not recommended_ using this in general. A better approach would be to use `Error.withMessageAndCause` to give an explicit context along with the cause.

```scala mdoc:reset
import eu.timepit.refined.types.all._
import io.isomarcte.errors4s.core._
import scala.util.Try

// From some hypothetical parsing library

final class InvalidIntException extends RuntimeException

def parseInt(value: String): Int =
    try {
        value.toInt
    } catch {
        case _: Throwable => throw new InvalidIntException
    }

// In our code

def add(a: String, b: String): Either[Throwable, Int] =
    (for {
      c <- Try(parseInt(a))
      d <- Try(parseInt(b))
    } yield c + d).fold(
        e => Left(Error.withMessageAndCause(NonEmptyString("Error During Addition Operation"), e)),
        i => Right(i)
    )

add("1", "a")
```

The observant reader probably also noticed that both `Error.fromThrowable` and `Error.withMessageAndCause` inserted the underlying `InvalidIntException` into the `causes` `Vector`. `causes` is similar to `getCause` on `Throwable` except that it allows the modeling of more than one cause. When you invoke `getCause` on `Error` you will get either the first error in the `Vector` or `null` (to comply with the `Throwable` API).

When working with a domain specific error you extend `Error` just as you might extend `RuntimeException`.

```scala mdoc

sealed trait OpError extends Error
object OpError {
    case class ParseError(context: String) extends OpError {
        override val primaryErrorMessage: NonEmptyString = NonEmptyString("Error during parsing")
        override val secondaryErrorMessages: Vector[String] = Vector(s"Context: $context")
    }
}
```

Or if your domain is simple enough an out of the box default error `SimpleError` is provided.

```scala mdoc
Error.SimpleError(NonEmptyString("Error during parsing"), Vector.empty, Vector.empty)
```

In fact the various `withMessage` functions on the `Error` companion object, e.g. `Error.withMessage` or `Error.withMessageAndCause`, are just wrappers on `Error.SimpleError`.

## HTTP ##

The http module provides a subtype of `Error`, `HttpError`. This type implements the structure defined in [rfc-7807][rfc-7807]. Strictly speaking, `HttpError` is a bit _more restrictive_ than [RFC 7807][rfc-7807] requires. For a truly accurate mapping you can use the related `HttpProblem` type, but this is discouraged for anything other than parsing. Both types have a trivial implementation included in their companion objects. If you don't need extension keys in your [RFC 7807][rfc-7807] JSON, then these types are perfectly fine to use directly.

This module does not specify an particular HTTP library and thus should be able to be integrated into any JVM HTTP library, nor does it specify a specific serialization library or format. As such, it is not very useful on its own. You'll probably want to look at the `http-circe` or `http4s-circe` modules. (PRs are welcome to add support for other JSON/XML libraries).

## HTTP Circe ##

This module adds serialization support for the `HttpError` and `HttpProblem` types via [Circe][circe] codecs for both of these traits as well as their trivial implementations. It also provides two types `ExtensibleCirceHttpError` and `ExtensibleCirceHttpProblem` which are the same as `HttpError` and `HttpProblem` except that they also include a reference to their own JSON representation. Extending these traits allows for adding [RFC 7807][rfc-7807] extension keys. That being said, if you don't need extension keys using the more simple `HttpError` type is recommended.

Users of this library can use the `SimpleCirceHttpError` or `SimpleCirceHttpProblem` types directly, or mix in `CirceHttpError` or `CirceHttpProblem` into their own error ADTs.

[circe]: https://github.com/circe/circe "Circe"

## http4s Circe ##

The http4s circe module provides middlewares which operate on `HttpError` or `HttpProblem` values.

### CirceHttpErrorToResponse ####

`CirceHttpErrorToResponse` is a server middleware which automatically transforms any `HttpError` or `HttpProblem` types into the appropriate response structures as defined in [rfc-7807][rfc-7807].

### PassthroughCirceHttpError ###

`PassthroughCirceHttpError` is a client middleware which checks for `application/problem+json` responses, and if it finds one decodes it and re-raises it in the current `F` context. You can combine this with `CirceHttpErrorToResponse` in order to have a http service passthrough errors. _Caution_, you should only do this if trust the downstream service which may be generating the `application/problem+json`.

[rfc-7807]: https://tools.ietf.org/html/rfc7807 "RFC 7807"

[refined]: https://github.com/fthomas/refined "Refined"
