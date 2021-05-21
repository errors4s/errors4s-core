package org.errors4s.core

/** An error type which is guaranteed to be useful.
  *
  * In the Scala ecosystem, for better or worse, most errors end up getting
  * converted to a subtype of [[java.lang.Throwable]], usually
  * [[java.lang.RuntimeException]]. This is convenient, because it
  * interoperates well with the JVM ecosystem, but it has several notable
  * drawbacks.
  *
  * The primary draw back is that this valid instance of
  * [[java.lang.RuntimeException]],
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
  * [[Error]] is attempting to be the nicer version of
  * [[java.lang.RuntimeException]], which is compatible with all existing
  * error code and always gives you meaningful context about the error.
  *
  * There are three common idioms in Scala code for handling errors.
  *
  *   - Create a custom Error ADT and then convert it to
  *     [[java.lang.Throwable]] when needs be (which is not always the case).
  *   - Extend [[java.lang.RuntimeException]] (or sometimes
  *     [[java.lang.Exception]]) and raise/throw the error.
  *   - Create a `new RuntimeException(<message>)` and raise/throw that value directly.
  *
  * [[Error]] aims to support all these cases.
  *
  * It is an open `trait`, so custom Error ADTs can extend it. This gives
  * error handling code the ability to be generic when needed, but still
  * guarantees a reasonable error message.
  *
  * Using the provided trivial implementation of [[Error]], you can convert
  * your ADT type into a [[Error]] when needed or you can just create
  * instances of [[Error]] directly and raise them, similar to `new
  * RuntimeException(<message>).
  *
  * [[Error]] also supports annotating [[Error#causes]], similar to
  * [[java.lang.Throwable#getCause]], but more expressive. Rather than having
  * a single `cause` (which may be `null`) [[Error]] allows for an arbitrary
  * number of causes.
  *
  * Finally, since [[Error]] extends [[java.lang.RuntimeException]], it can
  * dropped into place with any code currently using [[java.lang.Throwable]] as the
  * default bottom error type.
  */
trait Error extends RuntimeException {

  /** The primary error message for contexts in which a single value is
    * required, e.g. [[java.lang.Throwable#getMessage]].
    *
    * For code which understands [[org.errors4s.core.Error]] it is
    * recommended to use [[#errorMessages]] instead. This gives you ''all''
    * the errors.
    */
  def primaryErrorMessage: NonEmptyString

  /** A set of secondary error messages. Often these are values interpolated
    * from the given context. This is why they are [[java.lang.String]] values
    * and not `cats.data.NonEmptyString` values.
    */
  def secondaryErrorMessages: Vector[String] = Vector.empty

  /** A set of causes for this [[org.errors4s.core.Error]]. The first
    * value in this set, if any, will be the value returned by
    * [[org.errors4s.core.Error#getCause]].
    */
  def causes: Vector[Throwable] = Vector.empty

  /** An error message for each values in [[#causes]]. This is just a
    * convenience method for aggregating all error messages.
    */
  final lazy val causesErrorMessages: Vector[String] = causes.map(value => Error.errorMessageFromThrowable(value).value)

  /** All the error messages for this [[org.errors4s.core.Error]]. This
    * includes the [[#primaryErrorMessage]], the [[#secondaryErrorMessages]],
    * and all error messages from [[#causesErrorMessages]].
    */
  final lazy val errorMessages: Vector[String] =
    Vector(primaryErrorMessage.value) ++ secondaryErrorMessages ++ causesErrorMessages

  final lazy val getMessageNes: NonEmptyString = {
    val nonPrimaryErrorMessages: Vector[String] = (secondaryErrorMessages ++ causesErrorMessages)
    nonPrimaryErrorMessages.size match {
      case size if size <= 0 =>
        primaryErrorMessage
      case _ =>
        primaryErrorMessage ++ (", " ++ nonPrimaryErrorMessages.mkString(", "))
    }
  }

  final override lazy val getMessage: String = getMessageNes.value

  final override lazy val getCause: Throwable = causes.headOption.getOrElse(null)
}

object Error {

  /** The trivial implementation of [[Error]].
    *
    * This is hidden to avoid binary compatibility issues.
    */
  final private[this] case class ErrorImpl(
    override val primaryErrorMessage: NonEmptyString,
    override val secondaryErrorMessages: Vector[String],
    override val causes: Vector[Throwable]
  ) extends Error {
    final override lazy val toString: String =
      s"Error(primaryErrorMessage = ${primaryErrorMessage}, secondaryErrorMessages = ${secondaryErrorMessages}, causes = ${causes})"
  }

  /** Create an [[Error]] from an error message. */
  def withMessage(errorMessage: NonEmptyString): Error = ErrorImpl(errorMessage, Vector.empty, Vector.empty)

  /** Create an [[Error]] from an error message and a single secondary error message. */
  def withMessages(errorMessage: NonEmptyString, secondaryErrorMessage: String): Error =
    ErrorImpl(errorMessage, Vector(secondaryErrorMessage), Vector.empty)

  /** Create an [[Error]] from an error message and a set of secondary error messages. */
  def withMessages_(errorMessage: NonEmptyString, secondaryErrorMessages: Vector[String]): Error =
    ErrorImpl(errorMessage, secondaryErrorMessages, Vector.empty)

  /** Create an [[Error]] from an error message and a [[java.lang.Throwable]] cause. */
  def withMessageAndCause(errorMessage: NonEmptyString, cause: Throwable): Error =
    ErrorImpl(errorMessage, Vector.empty, Vector(cause))

  /** Create an [[Error]] from an error message, a secondary error message, and a [[java.lang.Throwable]] cause. */
  def withMessagesAndCause(errorMessage: NonEmptyString, secondaryErrorMessage: String, cause: Throwable): Error =
    ErrorImpl(errorMessage, Vector(secondaryErrorMessage), Vector(cause))

  /** Create an [[Error]] from an error message, a secondary error message, and set of [[java.lang.Throwable]] causes. */
  def withMessagesAndCauses(
    errorMessage: NonEmptyString,
    secondaryErrorMessage: String,
    causes: Vector[Throwable]
  ): Error = ErrorImpl(errorMessage, Vector(secondaryErrorMessage), causes)

  /** Create an [[Error]] from an error message, a set of secondary error messages, and a set of [[java.lang.Throwable]] causes. */
  def withMessagesAndCauses_(
    errorMessage: NonEmptyString,
    secondaryErrorMessages: Vector[String],
    causes: Vector[Throwable]
  ): Error = ErrorImpl(errorMessage, secondaryErrorMessages, causes)

  /** Create an [[Error]] from an arbitrary [[java.lang.Throwable]].
    *
    * @note Use of this method is discouraged unless you are in a situation
    *       where you don't have any context at all. Otherwise use
    *       [[#withMessageAndCause]].
    */
  def fromThrowable(t: Throwable): Error = ErrorImpl(errorMessageFromThrowable(t), Vector.empty, Vector(t))

  /** Generate a `NonEmptyString` error message from any given [[java.lang.Throwable]].
    *
    * This is first attempt to use
    * [[java.lang.Throwable#getLocalizedMessage]], falling back to the
    * [[java.lang.Class#getCanonicalName]], then [[java.lang.Class#getName]],
    * finally if either of those yield the empty [[java.lang.String]] (which
    * should be impossible), a default `NonEmptyString` is used.
    */
  def errorMessageFromThrowable(t: Throwable): NonEmptyString =
    t match {
      case t: Error =>
        t.getMessageNes
      case _ =>
        NonEmptyString
          .from(
            // Handle possible null value
            Option(t.getLocalizedMessage()).getOrElse(nameOf(t))
          )
          .getOrElse(
            // Normally we'd use the nes interpolator here, but since it is a
            // macro we can't use it in the same compilation unit.
            NonEmptyString.unsafe(
              "Unknown Error: getLocalizedMessage was null and we could not get the class name. This is probably a bug in errors4s."
            )
          )
    }

  /** Get the name of anything.
    *
    * @note only use this function to attempt to provide information about a
    *       problem if everything else has failed. That is, if at all possible
    *       don't use this method. It is intended to help generate useful
    *       information about an error in the case where no other useful
    *       information was provided, e.g. when
    *       `Throwable.getLocalizedMessage` is `null`.
    */
  def nameOf(value: Any): String = {
    val c: Class[_] = value.getClass
    Option(c.getCanonicalName).getOrElse(c.getName)
  }
}
