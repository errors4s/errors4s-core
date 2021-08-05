# Errors For Scala (Core) #

# ScalaDoc #

* [core][core-javadoc]
* [cats][cats-javadoc]
* [scalacheck][scalacheck-javadoc]

[core-javadoc]: https://www.javadoc.io/doc/org.errors4s/errors4s-core_2.13/1.0.0.0-RC0/index.html "Core Scaladoc"
[cats-javadoc]: https://www.javadoc.io/doc/org.errors4s/errors4s-core-cats_2.13/1.0.0.0-RC0/index.html "Cats Scaladoc"
[scalacheck-javadoc]: https://www.javadoc.io/doc/org.errors4s/errors4s-core-scalacheck_2.13/1.0.0.0-RC0/index.html "Scalacheck Scaladoc"

# Overview #

Errors4s is a family of projects which attempt to provide a better base error type than `java.lang.Throwable`. The foundation for which is the `org.errors4s.core.Error` type.

This project provides three built in modules.

* core
    * Fundamental datatypes
* cats
    * Cats instances for core
* scalacheck
    * Scalacheck instances for core

## Using ##

Add this to your `libraryDependencies` in your `build.sbt`.

```scala
    "org.errors4s" %% "errors4s-core" % "1.0.0.0-RC0"
```

## How is this different from Throwable? ##

`Error` extends `java.lang.RuntimeException` thus it is a `java.lang.Throwable` and can be used as a drop in replacement in any system operation on `java.lang.Throwable` as the fundamental error type, however `Error` provides a number of features beyond that of `java.lang.Throwable`. The intent of these features is to give the developer the ability to more clearly express error states and gently push the developer to always provide useful information in the event of an application error.

In short, the additional features provided by `Error` are,

* A requirement that a primary error message `String` be given and that it have a non-zero length, e.g. _disallowing_ `new RuntimeException("")` and providing now constructor analogous to `new RuntimeException()` (with no arguments).
* The ability to provide an arbitrary number of secondary error messages.
* The ability to provide an arbitrary number of causes. This is similar to `getCause` from `Throwable`, but allows handling situations when there is more than one cause.
* Built in tooling aggregate primary and secondary error messages and secondary causes into a single message. This becomes the value of `getMessage` from the `Throwable` type.
* A number of useful default methods to create default implementations of `Error`.

For the `errors4s-core` type, that's about it. The other projects in the errors4s ecosystem provide many additional utilities built on top of this small foundation.

## Okay, but why not just use Throwable? ##

This project was developed out of frustration of trying to deal with production errors which lacked any meaningful error message. All too often I would encounter exceptions with a `null` or `""` error message. By creating an API which nudges the developer to be more descriptive about error situations, while still being easy to use, one can help reduce these situations. Overtime it also grew to be able to treat errors as immutable data with the ability to express more complex error situations. This proved to be a good foundation on which to build the other errors4s projects.

If these situations are not something which concerns you and you have no use for the more expressive API of `Error` or that of the sub-projects, then there is little use for you to use this type over `Throwable`.

# API Overview #

## Core ##

### NonEmptyString ###

Before we look at the API of `Error` itself, we should take a brief look at the API for `NonEmptyString`.

In version <= 0.1.x of this project the `NonEmptyString` type from the excellent [refined][refined] project was used. However in an effort to keep the dependencies of this project as small as possible and also to be able to express some more complex use cases, such as interpolation into a `NonEmptyString` with compile time literals, as of version 1.0.0.0 this project provides its own `org.errors4s.core.NonEmptyString` data type. This data type is primarily used for the `primaryErrorMessage` of each `Error`.

`NonEmptyString` values can not be directly created at runtime. The only method to directly create them is `from` which returns an `Either[String, NonEmptyString]`, which is `Left` if the given `String` is `null` or `""`.

```scala
import org.errors4s.core._

NonEmptyString.from("")
// res0: Either[String, NonEmptyString] = Left(Unable to create NonEmptyString from empty string value.)
NonEmptyString.from(null)
// res1: Either[String, NonEmptyString] = Left(Given String value was null. This is not permitted for NonEmptyString values.)
NonEmptyString.from("A non-empty string")
// res2: Either[String, NonEmptyString] = Right(A non-empty string)
```

This is a somewhat cumbersome way to create `NonEmptyString` values, especially if we are using them for error messages. We don't want to always handle the `Left` branch of this `Either` when we are certain we are providing non-empty values.

Thankfully, `NonEmptyString` provides two mechanisms to safely create instances without having to go through `Either` as long as some part of the underlying `String` is known at compile time to be a non-empty literal value. These mechanisms work in **both** Scala 2 and 3.

The first is the `apply` method. This method uses a compile time macro (different ones for Scala 2 and 3) to check that the given `String` is a non-empty literal value. If it is, then it lifts it into a `NonEmptyString` instance, if it isn't then it yields a _compilation error_. For example,

```scala
NonEmptyString("A non-empty string")
// res3: NonEmptyString = NonEmptyStringImpl(value = "A non-empty string")
```

This works well for many situations, but sometimes we want to provide some runtime context in our `NonEmptyString`. For that we can use the `nes` interpolator. The `nes` interpolator allows us to interpolate arbitrary values into our `NonEmptyString` as long as _at least some part of it is a non-empty string literal at compile time_. To use this we need to import `syntax.all` (or `syntax.nes`). For example,

```scala
import org.errors4s.core.syntax.all._

val port: Int = 70000
// port: Int = 70000

nes"Invalid port number: ${port}"
// res4: NonEmptyString = NonEmptyStringImpl(
//   value = "Invalid port number: 70000"
// )
```

Once you have a `NonEmptyString` value you can also add arbitrary other `String` values to it, while retaining the `NonEmptyString`. Thus an alternative way to encode the above expression could have been,

```scala
val base: NonEmptyString = NonEmptyString("Invalid port number: ")
// base: NonEmptyString = NonEmptyStringImpl(value = "Invalid port number: ")

val value: NonEmptyString = base :+ port.toString
// value: NonEmptyString = NonEmptyStringImpl(
//   value = "Invalid port number: 70000"
// )
```

### Error ###

`Error` is provided as a `trait` so that it can be extended to provide the base for specialized error types, but it's companion object also provides methods to create instances of `Error` directly if you do not need anything fancy.

They are all pretty straight forward, effectively allowing convenient access to all the permutations of an `Error` encoding.

```scala
Error.withMessage(nes"An error has occurred")
// res5: Error = Error(primaryErrorMessage = An error has occurred, secondaryErrorMessages = Vector(), causes = Vector())
Error.withMessages(nes"An error has occurred", "It was very bad")
// res6: Error = Error(primaryErrorMessage = An error has occurred, secondaryErrorMessages = Vector(It was very bad), causes = Vector())
Error.withMessagesAndCause(nes"An error has occurred", "It was very bad", Error.withMessage(nes"This was the cause"))
// res7: Error = Error(primaryErrorMessage = An error has occurred, secondaryErrorMessages = Vector(It was very bad), causes = Vector(Error(primaryErrorMessage = This was the cause, secondaryErrorMessages = Vector(), causes = Vector())))
```

As mentioned above, `getMessage` aggregates the entire error context together. For example,

```scala
Error.withMessagesAndCause(nes"An error has occurred", "It was very bad", Error.withMessage(nes"This was the cause")).getMessage
// res8: String = Primary Error: An error has occurred, Secondary Errors(It was very bad), Causes(Primary Error: This was the cause)
```

## Scalacheck ##

Scalacheck instances for the types in `core` are provided in the `scalacheck` module. If you'd like to use them in your project you can add this to your `libraryDependencies`.

```scala
    "org.errors4s" %% "errors4s-core-scalacheck" % "1.0.0.0-RC0"
```

The instances provided here are [orphan][orphan] instances. To use them you need to import the `org.errors4s.core.scalacheck.instances._` package. You will also need to have an underlying implicit [Arbitrary][scalacheck-arbitrary] or [Cogen][scalacheck-cogen] in scope.

```scala
import org.errors4s.core._
import org.errors4s.core.scalacheck.instances._
import org.scalacheck._

val arbitraryNES: Arbitrary[NonEmptyString] = implicitly[Arbitrary[NonEmptyString]]
```

[orphan]: https://wiki.haskell.org/Orphan_instance "Orphan"

## Cats ##

Instances of various [Cats][cats] typeclasses for the types in `core` are provided in the `cats` module. If you'd like to use them in your project you can add this to your `libraryDependencies`.

```scala
    "org.errors4s" %% "errors4s-core-cats" % "1.0.0.0-RC0"
```

The instances provided here are [orphan][orphan] instances. To use them you need to import the `org.errors4s.core.cats.instances._` package.

```scala
import cats._
import org.errors4s.core._
import org.errors4s.core.cats.instances._

val catsOrderForNonEmptyString: Order[NonEmptyString] = implicitly[Order[NonEmptyString]]
```

[cats]: https://github.com/typelevel/cats "Cats"

# Version Matrix #

This project uses [Package Versioning Policy (PVP)][pvp]. This is to allow long term support on a binary compatible release. (see the discussion at the end of the README)

If you need support for a version combination which is not listed here, please open an issue and we will endeavor to add support for it if possible.

|Version|Cats Version|Scalacheck Version|Scala 2.11|Scala 2.12|Scala 2.13|Scala 3.0|
|-------|:----------:|:----------------:|:--------:|:--------:|:--------:|:-------:|
|1.0.x.x|2.x.x       |1.x.x (>= 1.15.x) |No        |Yes       |Yes       |Yes      |

# Versioning #

## PVP ##

This project follows [Package Versioning Policy (PVP)][pvp] for versioning. This is similar to [Semantic Versioning (semver)][semver] with a couple small differences. The most important differences are that the first _two_ version numbers both correspond to the major version, and thus both indicate potentially binary breaking changes, rather than just the first number as in [semver][semver]. The other important difference is that there are some other circumstances which imply a major version change beyond just binary compatibility, the most common such circumstance in Scala being the deprecation of a symbol (method/function/type/class/trait/etc.).

For a more detailed overview see [this writeup][sbt-version-scheme-enforcer-pvp], as well as the official documentation for [pvp][pvp].

## Why? ##

By convention in this project (and the other errors4s projects) the _first_ version number indicates a binary compatible dependency set and the _second_ version number indicates internal binary compatibility. This is not strictly specified in [pvp][pvp], but is compatible with it ([pvp][pvp] doesn't dictate when you should change the first or second version number).

The reason this project chooses to use [pvp][pvp] rather than the more common [Early Semver][early-semver] is so that we can provide longer support to our users.

For example, if a dependency of an errors4s project makes a binary breaking change we can release a new version with an incremented _first_ version number, but still keep releasing versions for the previous dependency set as well. With [semver][semver] or [early-semver][early-semver] you can only release updates to non-head branches which are non-breaking in _any way_, but with [pvp][pvp] we are free to break our internal binary API if we absolutely need to do so, even on old branches. It should be noted that this project goes to extreme lengths to _not break our binary API in any case_, but being able to do so means we can realistically support old branches indefinitely.

[refined]: https://github.com/refined "refined"

[pvp]: https://pvp.haskell.org/ "PVP"

[semver]: https://semver.org/ "Semver"

[sbt-version-scheme-enforcer-pvp]: https://github.com/isomarcte/sbt-version-scheme-enforcer/#why-does-this-plugin-exist "SBT Version Scheme Enforcer"

[early-semver]: https://scala-lang.org/blog/2021/02/16/preventing-version-conflicts-with-versionscheme.html "Early Semver"
