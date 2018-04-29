package com.sony.utils

import org.json4s
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods.{compact, render}

object TransFormJson {
  def getTransformedJson(value: JValue): JValue = {
    val transFormedValue = value transformField {
      case JField("product_id", JString(s)) => (ChangeConstant.getMappings("product_id"), JString(s))
      case JField("ReviewerLocation", JString(s)) => (ChangeConstant.getMappings("ReviewerLocation"), JString(s))
      case JField("DisplayLocale", JString(s)) => (ChangeConstant.getMappings("DisplayLocale"), JString(s))
      case JField("Featured", JString(s)) => (ChangeConstant.getMappings("Featured"), JBool(s.toBoolean))
      case JField("RatingsOnly", JString(s)) => (ChangeConstant.getMappings("RatingsOnly"), JBool(s.toBoolean))
      case JField("Recommended", JString(s)) => (ChangeConstant.getMappings("Recommended"), JBool(s.toBoolean))
      case JField("NumFeedbacks", JString(s)) => (ChangeConstant.getMappings("NumFeedbacks"), JInt(s.toInt))
      case JField("NumNegativeFeedbacks", JString(s)) => (ChangeConstant.getMappings("NumNegativeFeedbacks"), JInt(s.toInt))
      case JField("NumPositiveFeedbacks", JString(s)) => (ChangeConstant.getMappings("NumPositiveFeedbacks"), JInt(s.toInt))
      case JField("ReviewerNickname", JString(s)) => (ChangeConstant.getMappings("ReviewerNickname"), JString(s))
      case JField("Rating", JString(s)) => (ChangeConstant.getMappings("Rating"), JInt(s.toInt))
      case JField("RatingRange", JString(s)) => (ChangeConstant.getMappings("RatingRange"), JInt(s.toInt))
      case JField("SendEmailAlertWhenPublished", JString(s)) => (ChangeConstant.getMappings("SendEmailAlertWhenPublished"),
        JBool(s.toBoolean))
      case JField("SendEmailAlertWhenCommented", JString(s)) => (ChangeConstant.getMappings("SendEmailAlertWhenCommented"),
        JBool(s.toBoolean))
    }

    val badge = transFormedValue \ "Badges" \ "Badge"
    val name = compact(render(badge \ "Name"))
    val authorId = transFormedValue \ "UserProfileReference" \ "id"
    val mergedContent = transFormedValue \ "Badges" removeField {
      _ == JField("Badge", badge)
    } merge {
      val changedName = if (name.length > 1) name.substring(1, name.length - 1) else name
      JObject(changedName -> badge)
    }

    val contextDataValues = transFormedValue \\ "ContextDataValues" \\ "ContextDataValue"
    var valueMap: Map[String, ContextDataValueDetails] = Map.empty

    val valueOrders: List[json4s.JValue] = contextDataValues.children.filter(x => x.isInstanceOf[JObject]) map { dataValue =>
      val id = dataValue \ "ContextDataDimension" \ "id"
      val details = ContextDataValueDetails(compact(render(dataValue \ "id")),
        compact(render(dataValue \ "ContextDataDimension" \ "ExternalId")))
      valueMap += (compact(render(id)) -> details)
      id
    }

    val clientResponse = JArray(List(transFormedValue \ "ClientResponses" \ "ClientResponse"))

    var emptyDataValue = transFormedValue \ "ContextDataValues" removeField {
      _ == JField("ContextDataValue", contextDataValues)
    }
    valueOrders foreach { order =>
      val key = compact(render(order))
      val details = valueMap(key)
      emptyDataValue = emptyDataValue merge {
        val changedKey = if (key.length > 1) key.substring(1, key.length - 1) else key
        JObject(changedKey -> JObject("Value" -> JString(details.value), "Id" -> JString(details.id)))
      }
    }

    val ratingValues = transFormedValue \\ "RatingValues" \\ "RatingValue"
    var ratingValueMap: Map[String, RatingValueDetails] = Map.empty
    val ratingOrders: List[json4s.JValue] = ratingValues.children.filter(x => x.isInstanceOf[JObject]) map { ratingValue =>
      val id = ratingValue \ "RatingDimension" \ "ExternalId"
      val value = compact(render(ratingValue \ "Rating")).toInt
      val valueRange = compact(render(ratingValue \ "RatingDimension" \ "RatingRange")).toInt
      val xmlLabel = compact(render(ratingValue \ "RatingDimension" \ "Label"))
      val xmlValueLabel = compact(render(ratingValue \ "RatingDimension" \ "Label1"))
      val displayType = compact(render(ratingValue \ "RatingDimension" \ "displayType"))
      val label = Option(xmlLabel)
      val minLabel = Option(xmlLabel)
      val maxLabel = Option(xmlLabel)
      val valueLabel = Option(xmlValueLabel)
      val details = RatingValueDetails(value, compact(render(id)), valueRange, maxLabel, displayType,
        label, minLabel, valueLabel)
      ratingValueMap += (compact(render(id)) -> details)
      id
    }

    var emptyRatingValue = transFormedValue \ "RatingValues" removeField {
      _ == JField("RatingValue", ratingValues)
    }
    ratingOrders foreach { order =>
      val key = compact(render(order))
      val details = ratingValueMap(key)
      val maxLabel = if (details.maxLabel.isDefined) JString(details.maxLabel.get) else JNull
      val minLabel = if (details.minLabel.isDefined) JString(details.minLabel.get) else JNull
      val label = if (details.label.isDefined) JString(details.label.get) else JNull
      val valueLabel = if (details.valueLabel.isDefined) JString(details.valueLabel.get) else JNull
      emptyRatingValue = emptyRatingValue merge {
        val changedKey = if (key.length > 1) key.substring(1, key.length - 1) else key
        JObject(changedKey -> JObject("Value" -> JInt(details.value),
          "Id" -> JString(details.id), "ValueRange" -> JInt(details.valueRange),
          "MaxLabel" -> maxLabel, "DisplayType" -> JString(details.displayType),
          "Label" -> label, "MinLabel" -> minLabel, "ValueLabel" -> valueLabel))
      }
    }

    val result: JValue = transFormedValue merge {
      JObject("SourceClient" -> JString("sony-global"))
    } removeField {
      _ == JField("Badges", transFormedValue \ "Badges")
    } merge {
      JObject("Badges" -> mergedContent)
    } merge {
      JObject("BadgesOrder" -> JArray(List(badge \ name)))
    } removeField {
      _ == JField("ClientResponses", transFormedValue \ "ClientResponses")
    } merge {
      JObject("ClientResponses" -> clientResponse)
    } merge {
      JObject("AuthorId" -> authorId)
    } merge {
      JObject("ContextDataValuesOrder" -> JArray(valueOrders))
    } removeField {
      _ == JField("ContextDataValues", transFormedValue \ "ContextDataValues")
    } merge {
      JObject("ContextDataValues" -> emptyDataValue)
    } merge {
      JObject("SecondaryRatingsOrder" -> JArray(ratingOrders))
    } removeField {
      _ == JField("RatingValues", transFormedValue \ "RatingValues")
    } merge {
      JObject("SecondaryRatings" -> emptyRatingValue)
    }
    result
  }
}

case class ContextDataValueDetails(value: String, id: String)

case class RatingValueDetails(value: Int, id: String, valueRange: Int,
                              maxLabel: Option[String], displayType: String,
                              label: Option[String], minLabel: Option[String],
                              valueLabel: Option[String])