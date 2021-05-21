package org.errors4s.http.circe

import cats.syntax.all._
import io.circe._
import io.circe.syntax._
import org.errors4s.http._

trait ExtensibleCirceHttpProblem extends HttpProblem {

  /** Additional fields beyond those defined in [[HttpProblem]].
    *
    * @note This [[io.circe.JsonObject]] should ''not'' contain keys which
    *       will conflict with the canonical fields in [[HttpProblem]].
    */
  def additionalFields: JsonObject = JsonObject.empty
}

object ExtensibleCirceHttpProblem {

  final private[this] case class ExtensibleCirceHttpProblemImpl(
    override val `type`: Option[String],
    override val title: Option[String],
    override val status: Option[Int],
    override val detail: Option[String],
    override val instance: Option[String],
    override val additionalFields: JsonObject
  ) extends ExtensibleCirceHttpProblem {
    final override lazy val toString: String =
      s"ExtensibleCirceHttpError(type = ${`type`}, title = $title, status = $status, detail = $detail, instance = $instance, additionalFields = ${additionalFields.toString}"
  }

  def simpleWithAdditionalFields[F[_]](
    `type`: Option[String],
    title: Option[String],
    status: Option[Int],
    detail: Option[String],
    instance: Option[String],
    additionalFields: JsonObject
  ): ExtensibleCirceHttpProblem =
    ExtensibleCirceHttpProblemImpl(`type`, title, status, detail, instance, filterRestrictedKeys(additionalFields))

  def simple[F[_]](
    `type`: Option[String],
    title: Option[String],
    status: Option[Int],
    detail: Option[String],
    instance: Option[String]
  ): ExtensibleCirceHttpProblem = simpleWithAdditionalFields(`type`, title, status, detail, instance, JsonObject.empty)

  implicit def circeCodec: Codec[ExtensibleCirceHttpProblem] =
    Codec.from[ExtensibleCirceHttpProblem](
      Decoder.instance[ExtensibleCirceHttpProblem]((hcursor: HCursor) =>
        (
          hcursor.downField("type").as[Option[String]],
          hcursor.downField("title").as[Option[String]],
          hcursor.downField("status").as[Option[Int]],
          hcursor.downField("detail").as[Option[String]],
          hcursor.downField("instance").as[Option[String]],
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

  def fromHttpProblem(value: HttpProblem): ExtensibleCirceHttpProblem =
    value match {
      case value: ExtensibleCirceHttpProblem =>
        value
      case value =>
        simple(value.`type`, value.title, value.status, value.detail, value.instance)
    }
}
