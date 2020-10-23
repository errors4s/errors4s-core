package io.isomarcte.errors4s.core

import eu.timepit.refined.types.all._
import scala.util._

/** An error type which is guaranteed to be useful.
  *
  * In the Scala ecosystem, for better or worse, most errors end up getting
  * converted to a subtype of `Throwable`, usually `RuntimeException`. This is
  * convenient, because it interoperates well with the JVM ecosystem, but it
  * has several notable drawbacks.
  *
  * The primary draw back is that this valid instance of `RuntimeException`,
  *
  * {{{
  * scala> val e = new RuntimeException
  * val e = new RuntimeException
  * val e: RuntimeException = java.lang.RuntimeException
  *
  * scala> e.getMessage
  * e.getMessage
  * val res0: String = null
  *
  * scala> e.getCause
  * e.getCause
  * val res1: Throwable = null
  * }}}
  *
  * One would be hard pressed to find a less helpful error. As amazing as it
  * may seem, it is not uncommon to have exceptions like this in real world
  * production code.
  *
  * [[Error]] is attempting to be the nicer version of `RuntimeException`,
  * which is compatible with all existing error code and always gives you
  * meaningful context about the error.
  *
  * There are three common idioms in Scala code for handling errors.
  *
  *   - Create a custom Error ADT and then convert it to `Throwable` when needs be (which is not always the case).
  *   - Extend `RuntimeException` (or sometimes `Exception`) and raise/throw the error.
  *   - Create a `new RuntimeException(<message>)` and raise/throw that value directly.
  *
  * [[Error]] aims to support all these cases.
  *
  * It is an open `trait`, so custom Error ADTs can extend it. This gives
  * error handling code the ability to be generic when needed, but still
  * guarantees a reasonable error message.
  *
  * Using the provided trivial implementation of [[Error]],
  * [[Error#SimpleError]], you can convert your ADT type into a [[Error]] when
  * needed or you can just create instances of [[Error#SimpleError]] directly
  * and raise them, similar to `new RuntimeException(<message>).
  *
  * [[Error]] also supports annotating [[Error#causes]], similar to
  * `Throwable#getCause`, but more expressive. Rather than having a single
  * `cause` (which may be `null`) [[Error]] allows for an arbitrary number of
  * causes.
  *
  * Finally, since [[Error]] extends `RuntimeException`, it can dropped into
  * place with any code currently using `Throwable` as the default bottom
  * error type.
  */
trait Error extends RuntimeException {

  /** The primary error message for contexts in which a single value is
    * required, e.g. `Throwable#getMessage`.
    *
    * For code which understands `Error` it is recommended to use
    * [[#errorMessages]] instead. This gives you ''all'' the errors.
    */
  def primaryErrorMessage: NonEmptyString

  /** A set of secondary error messages. Often these are values interpolated
    * from the given context. This is why they are `String` values and not
    * `NonEmptyString` values.
    */
  def secondaryErrorMessages: Vector[String] = Vector.empty

  /** A set of causes for this [[Error]]. The first value in this set, if any,
    * will be the value returned by `Error.getCause`.
    */
  def causes: Vector[Throwable] = Vector.empty

  /** An error message for each values in [[#causes]]. This is just a
    * convenience method for aggregating all error messages.
    */
  final lazy val causesErrorMessages: Vector[String] = causes.map(value => Error.errorMessageFromThrowable(value).value)

  /** All the error messages for this [[Error]]. This includes the
    * [[#primaryErrorMessage]], the [[#secondaryErrorMessages]], and all error
    * messages from [[#causesErrorMessages]].
    */
  final lazy val errorMessages: Vector[String] =
    Vector(primaryErrorMessage.value) ++ secondaryErrorMessages ++ causesErrorMessages

  final override lazy val getMessage: String  = primaryErrorMessage.value
  final override lazy val getCause: Throwable = causes.headOption.getOrElse(null)
}

object Error {

  /** The trivial implementation of [[Error]]. */
  final case class SimpleError(
    override val primaryErrorMessage: NonEmptyString,
    override val secondaryErrorMessages: Vector[String],
    override val causes: Vector[Throwable]
  ) extends Error

  object SimpleError {
    def apply(primaryErrorMessage: NonEmptyString): SimpleError =
      SimpleError(primaryErrorMessage, Vector.empty, Vector.empty)
  }

  /** Create an [[Error]] from an error message. */
  def withMessage(errorMessage: NonEmptyString): SimpleError = SimpleError(errorMessage, Vector.empty, Vector.empty)

  /** Create an [[Error]] from an error message and a single secondary error message. */
  def withMessages(errorMessage: NonEmptyString, secondaryErrorMessage: String): SimpleError =
    SimpleError(errorMessage, Vector(secondaryErrorMessage), Vector.empty)

  /** Create an [[Error]] from an error message and a set of secondary error messages. */
  def withMessages_(errorMessage: NonEmptyString, secondaryErrorMessages: Vector[String]): SimpleError =
    SimpleError(errorMessage, secondaryErrorMessages, Vector.empty)

  /** Create an [[Error]] from an error message and a `Throwable` cause. */
  def withMessageAndCause(errorMessage: NonEmptyString, cause: Throwable): SimpleError =
    SimpleError(errorMessage, Vector.empty, Vector(cause))

  /** Create an [[Error]] from an error message, a secondary error message, and a `Throwable` cause. */
  def withMessagesAndCause(errorMessage: NonEmptyString, secondaryErrorMessage: String, cause: Throwable): SimpleError =
    SimpleError(errorMessage, Vector(secondaryErrorMessage), Vector(cause))

  /** Create an [[Error]] from an error message, a secondary error message, and set of `Throwable` causes. */
  def withMessagesAndCauses(
    errorMessage: NonEmptyString,
    secondaryErrorMessage: String,
    causes: Vector[Throwable]
  ): SimpleError = SimpleError(errorMessage, Vector(secondaryErrorMessage), causes)

  /** Create an [[Error]] from an error message, a set of secondary error messages, and a set of `Throwable` causes. */
  def withMessagesAndCauses_(
    errorMessage: NonEmptyString,
    secondaryErrorMessages: Vector[String],
    causes: Vector[Throwable]
  ): SimpleError = SimpleError(errorMessage, secondaryErrorMessages, causes)

  /** Create an [[Error]] from an arbitrary `Throwable`.
    *
    * @note Use of this method is discouraged unless you are in a situation
    *       where you don't have any context at all. Otherwise use
    *       [[#withMessageAndCause]].
    */
  def fromThrowable(t: Throwable): SimpleError = SimpleError(errorMessageFromThrowable(t), Vector.empty, Vector(t))

  /** We have to do this to work around a JRE 8 bug if we want to use
    * class.getSimpleName for a fallback error message. This is not an issue
    * on JRE >= 9. JDK 8 is EOL, so one would hope it is not being used
    * anymore, but sadly that is not the case.
    *
    * @see [[https://github.com/scala/bug/issues/2034]]
    */
  private[this] lazy val jreVersion: Try[Int] = {
    val versionParts: List[String] = System.getProperty("java.version").split("""\.""").toList.take(2)
    versionParts match {
      case ("1" :: version :: _) =>
        Try(version.toInt)
      case (version :: _) =>
        Try(version.toInt)
      case otherwise =>
        Failure(withMessages(NonEmptyString("Unexpected version format"), otherwise.toString))
    }
  }

  /** Determine if it is safe to call `java.lang.Class.getSimpleName`.
    *
    * @see [[https://github.com/scala/bug/issues/2034]]
    */
  private[this] lazy val isSafeToUseSimpleName: Boolean = this.jreVersion.fold(Function.const(false), _ > 8)

  /** Generate a `NonEmptyString` error message from any given `Throwable`.
    *
    * This is first attempt to use `getLocalizedMessage`, falling back to the
    * `getClass.getSimpleName` if the JRE >= 9 otherwise `getClass.getName`,
    * finally if either of those yield the empty `String` (which should be
    * impossible), a default `NonEmptyString` is used.
    *
    * @see [[https://github.com/scala/bug/issues/2034]]
    */
  def errorMessageFromThrowable(t: Throwable): NonEmptyString =
    t match {
      case t: Error =>
        NonEmptyString
          .from(t.errorMessages.mkString(", "))
          .getOrElse(t.primaryErrorMessage /* should not be possible */ )
      case _ =>
        NonEmptyString
          .from(
            // Handle possible null value
            Option(t.getLocalizedMessage()).getOrElse(nameOf(t))
          )
          .getOrElse(
            NonEmptyString(
              "Unknown Error: getLocalizedMessage was null and we could not get the class name. This is probably a bug in errors4s."
            )
          )
    }

  /** Get the name of anything. This will call `_.getClass.getSimpleName` on JRE
    * >= 9 and `_.getClass.getName`on JRE <= 8.
    *
    * @note only use this function to attempt to provide information about a
    *       problem if everything else has failed. That is, if at all possible
    *       don't use this method. It is intended to help generate useful
    *       information about an error in the case where no other useful
    *       information was provided, e.g. when
    *       `Throwable.getLocalizedMessage` is `null`.
    *
    * @see [[https://github.com/scala/bug/issues/2034]]
    */
  def nameOf(value: Any): String = {
    val c: Class[_] = value.getClass
    if (isSafeToUseSimpleName) {
      // Only safe on JRE >= 9
      c.getSimpleName()
    } else {
      // Safe on JRE <= 8, but much less readable.
      c.getName()
    }
  }
}
