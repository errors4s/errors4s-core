package org.errors4s.http.circe

import cats.syntax.all._
import io.circe._
import io.circe.syntax._
import org.errors4s.circe.instances._
import org.errors4s.core._
import org.errors4s.http._
import org.errors4s.http.circe.implicits.httpStatusCodec

trait ExtensibleCirceHttpError extends HttpError {

  /** Additional fields beyond those defined in [[HttpError]].
    *
    * @note This [[io.circe.JsonObject]] should ''not'' contain keys which
    *       will conflict with the canonical fields in [[HttpError]].
    */
  def additionalFields: JsonObject = JsonObject.empty
}

object ExtensibleCirceHttpError {

  final private[this] case class ExtensibleCirceHttpErrorImpl(
    override val `type`: NonEmptyString,
    override val title: NonEmptyString,
    override val status: HttpStatus,
    override val detail: Option[String],
    override val instance: Option[NonEmptyString],
    override val additionalFields: JsonObject
  ) extends ExtensibleCirceHttpError {
    final override lazy val toString: String =
      s"ExtensibleCirceHttpError(type = ${`type`}, title = $title, status = $status, detail = $detail, instance = $instance, additionalFields = ${additionalFields.toString}"
  }

  def simpleWithAdditionalFields[F[_]](
    `type`: NonEmptyString,
    title: NonEmptyString,
    status: HttpStatus,
    detail: Option[String],
    instance: Option[NonEmptyString],
    additionalFields: JsonObject
  ): ExtensibleCirceHttpError =
    ExtensibleCirceHttpErrorImpl(`type`, title, status, detail, instance, filterRestrictedKeys(additionalFields))

  def simple[F[_]](
    `type`: NonEmptyString,
    title: NonEmptyString,
    status: HttpStatus,
    detail: Option[String],
    instance: Option[NonEmptyString]
  ): ExtensibleCirceHttpError = simpleWithAdditionalFields(`type`, title, status, detail, instance, JsonObject.empty)

  implicit lazy val circeCodec: Codec[ExtensibleCirceHttpError] = Codec.from[ExtensibleCirceHttpError](
    Decoder.instance[ExtensibleCirceHttpError]((hcursor: HCursor) =>
      (
        hcursor.downField("type").as[NonEmptyString],
        hcursor.downField("title").as[NonEmptyString],
        hcursor.downField("status").as[HttpStatus],
        hcursor.downField("detail").as[Option[String]],
        hcursor.downField("instance").as[Option[NonEmptyString]],
        hcursor.value.as[JsonObject]
      ).mapN { case (typeValue, title, status, detail, instance, additionalFields) =>
        simpleWithAdditionalFields(typeValue, title, status, detail, instance, additionalFields)
      }
    ),
    Encoder.instance(value =>
      Json.fromJsonObject(
        JsonObject(
          "type"     -> value.`type`.asJson,
          "title"    -> value.title.asJson,
          "status"   -> value.status.asJson,
          "detail"   -> value.detail.asJson,
          "instance" -> value.instance.asJson
        ).deepMerge(filterRestrictedKeys(value.additionalFields))
      )
    )
  )

  def fromHttpError(value: HttpError): ExtensibleCirceHttpError =
    value match {
      case value: ExtensibleCirceHttpError =>
        value
      case value =>
        simpleWithAdditionalFields(
          value.`type`,
          value.title,
          value.status,
          value.detail,
          value.instance,
          JsonObject.empty
        )
    }
}
