package com.sony.utils

import com.mongodb.client.MongoCollection
import com.sony.services.{CollectionDocument, CollectionDocuments, CollectionUpdateDocument, DefaultActors}
import org.bson.Document
import org.bson.types.ObjectId
import org.json4s.JValue
import org.json4s.JsonAST.{JField, JInt, JObject}
import org.json4s.jackson.JsonMethods.{compact, render}

import scala.util.Random

trait MongoConnectConfig extends MongoConfig with FileConfig {
  def collection: String

  def lookupCollection: String

  def nativeReviewStatisticsCollection: String

  def reviewStatisticsCollection: String

  def reviewsCollection: String

  lazy val initialMap: List[MongoCollectionWithIndex] = mongoDatabaseList.zipWithIndex.map {
    db => {
      val coll = db._1.getCollection(collection)
      val lookupColl = db._1.getCollection(lookupCollection)
      val nativeReviewStatisticsColl = db._1.getCollection(nativeReviewStatisticsCollection)
      val reviewStatisticsColl = db._1.getCollection(reviewStatisticsCollection)
      val reviewsColl = db._1.getCollection(reviewsCollection)
      MongoCollectionWithIndex(db._2, Map[String, MongoCollection[Document]]((collection, coll), (lookupCollection, lookupColl)
        , (nativeReviewStatisticsCollection, nativeReviewStatisticsColl), (reviewStatisticsCollection, reviewStatisticsColl),
        (reviewsCollection, reviewsColl)))
    }
  }

  def insertData(value: JValue, objectId: ObjectId,actors:DefaultActors) = {
    val random = new Random().nextInt(MongoConstant.numberOfInstances)
    val newProduct = value \\ "Product"
    val finalProduct = newProduct removeField {
      _ == JField("Reviews", newProduct \\ "Reviews")
    } removeField {
      _ == JField("NativeReviewStatistics", newProduct \ "NativeReviewStatistics")
    } removeField {
      _ == JField("ReviewStatistics", newProduct \ "ReviewStatistics")
    }

    extractProduct(objectId, random, finalProduct,actors)

    extractLookup(objectId, random, finalProduct,actors)

    extractReview(objectId, random, newProduct,actors)

    extractNativeStatistics(objectId, random, newProduct,actors)

    extractReviewStatistics(objectId, random, newProduct,actors)
  }

  private def extractReviewStatistics(objectId: ObjectId, random: Int, newProduct: JValue,actors:DefaultActors) = {
    val reviewStatistics = newProduct \ "ReviewStatistics"
    val insertReviewResult = compact(render(reviewStatistics))
    val reviewStatisticsCheckSum = insertReviewResult.hashCode
    val lastReviewStatistics = reviewStatistics merge {
      JObject("checksum" -> JInt(reviewStatisticsCheckSum))
    }
    val reviewDoc: Document = Document.parse(compact(render(lastReviewStatistics)))
    val changedReviewDoc = reviewDoc.append("productId", objectId)
    actors.reviewStatisticsRef ! CollectionDocument(changedReviewDoc, initialMap(random).collections(reviewStatisticsCollection))
  }

  private def extractNativeStatistics(objectId: ObjectId, random: Int, newProduct: JValue,actors:DefaultActors) = {
    val nativeReviewStatistics = newProduct \ "NativeReviewStatistics"
    val insertNativeResult = compact(render(nativeReviewStatistics))
    val nativeCheckSum = insertNativeResult.hashCode
    val lastNativeReview = nativeReviewStatistics merge {
      JObject("checksum" -> JInt(nativeCheckSum))
    }
    val nativeDoc: Document = Document.parse(compact(render(lastNativeReview)))
    val changedDoc = nativeDoc.append("productId", objectId)
    actors.nativeReviewStatisticsRef ! CollectionDocument(changedDoc, initialMap(random).collections(nativeReviewStatisticsCollection))
  }

  private def extractReview(objectId: ObjectId, random: Int, newProduct: JValue,actors:DefaultActors) = {
    val reviews = newProduct \\ "Reviews" \\ "Review"
    val docs: List[Document] = reviews.children.filter(x => x.isInstanceOf[JObject]) map { review =>
      val newReview = TransFormJson.getTransformedJson(review)
      val insertReviewResult = compact(render(newReview))
      val reviewCheckSum = insertReviewResult.hashCode
      val lastReview = newReview merge {
        JObject("checksum" -> JInt(reviewCheckSum))
      }

      val doc: Document = Document.parse(compact(render(lastReview)))
      val newDoc = doc.append("productId", objectId)
      newDoc
    }
    actors.reviewRef ! CollectionDocuments(docs, initialMap(random).collections(reviewsCollection))
  }

  private def extractLookup(objectId: ObjectId, random: Int, finalProduct: JValue,actors:DefaultActors) = {
    val productId = compact(render(finalProduct \ "id"))
    val lookupDoc = new Document("productId", objectId).append("xmlProductId", productId.substring(1, productId.length - 1))
    actors.productLookupRef ! CollectionDocument(lookupDoc, initialMap(random).collections(lookupCollection))
  }

  private def extractProduct(objectId: ObjectId, random: Int, finalProduct: JValue,actors:DefaultActors) = {
    val insertResult = compact(render(finalProduct))
    val productCheckSum = insertResult.hashCode
    val lastProduct = finalProduct merge {
      JObject("checksum" -> JInt(productCheckSum))
    }
    val doc: Document = Document.parse(compact(render(lastProduct))).append("_id", objectId)
    actors.productRef ! CollectionDocument(doc, initialMap(random).collections(collection))
  }

  def updateData(value: JValue,actors:DefaultActors) = {
    val newProduct = value \\ "Product"
    val finalProduct = newProduct removeField {
      _ == JField("Reviews", newProduct \\ "Reviews")
    } removeField {
      _ == JField("NativeReviewStatistics", newProduct \ "NativeReviewStatistics")
    } removeField {
      _ == JField("ReviewStatistics", newProduct \ "ReviewStatistics")
    }
    val id = compact(render(finalProduct \ "id"))
    updateProduct(finalProduct, id,actors)
    val filteredDocument: Document = updateLookup(id,actors)
    updateReviewStatistics(newProduct, filteredDocument,actors)
    updateNativeReviewStatistics(newProduct, finalProduct, filteredDocument,actors)
    val reviews = newProduct \\ "Reviews" \\ "Review"
    updateReview(reviews, filteredDocument,actors)
  }

  private def updateLookup(id: String,actors:DefaultActors) = {
    val lookupColl = mongoDatabase.getCollection(lookupCollection)
    val filteredDoc = lookupColl.find(new Document("xmlProductId", id)).projection(new Document("productId", 1).append("_id", 0)).first()
    val filteredDocument: Document = if (filteredDoc == null) {
      val productId = ObjectId.get()
      val lookupDoc = new Document("xmlProductId", id).append("productId", productId)
      actors.productLookupRef ! CollectionDocument(lookupDoc, lookupColl)
      new Document("productId", productId)
    }
    else {
      filteredDoc
    }
    filteredDocument
  }

  private def updateNativeReviewStatistics(newProduct: JValue, finalProduct: JValue, filteredDocument: Document,actors:DefaultActors) = {
    val nativeColl = mongoDatabase.getCollection(nativeReviewStatisticsCollection)
    val nativeStatistics = newProduct \ "NativeReviewStatistics"
    val insertNativeResult = compact(render(nativeStatistics))
    val nativeReviewStatisticsCheckSum = insertNativeResult.hashCode
    val lastNativeStatistics = finalProduct merge {
      JObject("checksum" -> JInt(nativeReviewStatisticsCheckSum))
    }
    val nativeDoc: Document = Document.parse(compact(render(lastNativeStatistics)))
    val changedNativeDoc = nativeDoc.append("productId", filteredDocument.get("productId"))
    actors.nativeReviewStatisticsRef ! CollectionUpdateDocument(changedNativeDoc, filteredDocument,
      nativeColl, nativeReviewStatisticsCheckSum)
  }

  private def updateReviewStatistics(newProduct: JValue, filteredDocument: Document,actors:DefaultActors) = {
    val reviewStatisticsColl = mongoDatabase.getCollection(reviewStatisticsCollection)
    val reviewStatistics = newProduct \ "ReviewStatistics"
    val insertReviewResult = compact(render(reviewStatistics))
    val reviewStatisticsCheckSum = insertReviewResult.hashCode
    val lastReviewStatistics = reviewStatistics merge {
      JObject("checksum" -> JInt(reviewStatisticsCheckSum))
    }
    val reviewDoc: Document = Document.parse(compact(render(lastReviewStatistics)))
    val changedReviewDoc = reviewDoc.append("productId", filteredDocument.get("productId"))
    actors.reviewStatisticsRef ! CollectionUpdateDocument(changedReviewDoc, filteredDocument,
      reviewStatisticsColl, reviewStatisticsCheckSum)
  }

  private def updateReview(reviews: JValue, filteredDocument: Document,actors:DefaultActors) = {
    reviews.children.filter(x => x.isInstanceOf[JObject]) foreach { review =>
      val newReview = TransFormJson.getTransformedJson(review)
      val insertReviewResult = compact(render(newReview))
      val reviewCheckSum = insertReviewResult.hashCode
      val lastReview = newReview merge {
        JObject("checksum" -> JInt(reviewCheckSum))
      }
      val doc: Document = Document.parse(compact(render(lastReview)))
      val newDoc = doc.append("productId", filteredDocument.get("productId"))
      val reviewColl = mongoDatabase.getCollection(reviewsCollection)
      val id = compact(render(review \ "id"))
      actors.reviewRef ! CollectionUpdateDocument(newDoc, new Document("id", id), reviewColl, reviewCheckSum)
    }
  }

  private def updateProduct(finalProduct: JValue, id: String,actors:DefaultActors) = {
    val coll = mongoDatabase.getCollection(collection)
    val insertResult = compact(render(finalProduct))
    val productCheckSum = insertResult.hashCode
    val lastProduct = finalProduct merge {
      JObject("checksum" -> JInt(productCheckSum))
    }
    val doc: Document = Document.parse(compact(render(lastProduct)))
    actors.productRef ! CollectionUpdateDocument(doc, new Document("id", id), coll, productCheckSum)
  }
}

case class MongoCollectionWithIndex(index: Int, collections: Map[String, MongoCollection[Document]])
