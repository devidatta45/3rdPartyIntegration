package com.sony.services

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.UpdateOptions
import com.sony.utils.BaseActor
import org.bson.Document
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._


case class CollectionDocument(document: Document, collection: MongoCollection[Document])

case class CollectionUpdateDocument(document: Document, filterDoc: Document, collection: MongoCollection[Document], checkSum: Int)

case class CollectionDocuments(documents: List[Document], collection: MongoCollection[Document])

class ProductActor extends CollectionUpdate with BaseActor {
  override def normalExecution: Receive ={
    case cmd: CollectionDocument => {
      cmd.collection.insertOne(cmd.document)
    }
    case cmd: CollectionUpdateDocument => {
      insertOrUpdate(cmd)
    }
  }
}

class ReviewsActor extends CollectionUpdate with BaseActor {

  override def normalExecution: Receive = {
    case cmd: CollectionDocuments => {
      if (cmd.documents.nonEmpty) cmd.collection.insertMany(cmd.documents.asJava)
    }
    case cmd: CollectionUpdateDocument => {
      insertOrUpdate(cmd)
    }
  }
}

class ProductLookupActor extends CollectionUpdate with BaseActor {

  override def normalExecution: Receive = {
    case cmd: CollectionDocument => {
      cmd.collection.insertOne(cmd.document)
    }
    case cmd: CollectionUpdateDocument => {
      insertOrUpdate(cmd)
    }
  }
}

class NativeReviewStatisticsActor extends CollectionUpdate with BaseActor {

  override def normalExecution: Receive = {
    case cmd: CollectionDocument => {
      cmd.collection.insertOne(cmd.document)
    }
    case cmd: CollectionUpdateDocument => {
      insertOrUpdate(cmd)
    }
  }
}

class ReviewStatisticsActor extends CollectionUpdate with BaseActor {

  override def normalExecution: Receive = {
    case cmd: CollectionDocument => {
      cmd.collection.insertOne(cmd.document)
    }
    case cmd: CollectionUpdateDocument => {
      insertOrUpdate(cmd)
    }
  }
}

trait CollectionUpdate {
  val logger = LoggerFactory.getLogger(getClass)

  def insertOrUpdate(cmd: CollectionUpdateDocument) = {
    val checksum = cmd.collection.find(cmd.filterDoc).projection(new Document("checkSum", 1).append("_id", 0)).first()
    if (cmd.checkSum == checksum.getInteger("checkSum")) {
      logger.info("No Change")
    }
    else {
      cmd.collection.replaceOne(cmd.filterDoc, cmd.document, new UpdateOptions().upsert(true))
    }
  }
}