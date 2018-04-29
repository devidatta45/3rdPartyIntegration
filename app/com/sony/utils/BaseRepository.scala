package com.sony.utils

import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteConcern
import reactivemongo.bson._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by DDM on 19-04-2017.
  */

//All Model should extend this class
trait BaseEntity {
  val _id: String
}

trait BaseMongoRepository[T <: BaseEntity] {

  import MongoConstant._

  import ExecutionContext.Implicits.global

  def table: String

  val driver = new MongoDriver
  val connection = Try {
    driver.connection(List(server), options = MongoConnectionOptions(
      readPreference = ReadPreference.primary,
      writeConcern = WriteConcern.Default,
      authMode = ScramSha1Authentication
    ))
  }
  val futureConnection = Future.fromTry(connection)
  val db = futureConnection.flatMap(_.database(dbName, FailoverStrategy(100.milliseconds, 20, { n => n })))
  val collection: Future[BSONCollection] = db.map(_.collection[BSONCollection](table))

  def save(t: T)(implicit writer: BSONDocumentWriter[T], reader: BSONDocumentReader[T]): Future[Boolean] = {
    for {
      document <- collection.flatMap(_.insert(t).map(x => x.ok))
    } yield document
  }
}

trait BaseFileRepository[T <: BaseEntity]{
  def save(t: T): Future[Boolean] = {
    Future {
      println("Saving to File system")
      true
    }
  }
}