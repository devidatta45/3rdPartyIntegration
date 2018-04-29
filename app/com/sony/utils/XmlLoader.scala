package com.sony.utils

import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase

import scala.collection.mutable.ListBuffer
import scala.io.{BufferedSource, Source}

trait XmlLoader {
  def file: String

  lazy val sourceFile = Source.fromFile(file)
}

object EndLine extends XmlLoader {
  def getProductWithEndLine: List[Int] = {
    var lineNumber = 0
    val buffer: ListBuffer[Int] = new ListBuffer[Int]()
    sourceFile.getLines() foreach { line =>
      if (line.contains("</Product>")) {
        buffer += lineNumber
      }
      lineNumber += 1
    }
    buffer.toList
  }

  override def file: String = "./masterData/bv_sony-global_standard_client_feed.xml"
}

trait MongoConfig {
  import MongoConstant._
  val mongoDatabaseList: List[MongoDatabase] = ports map {
    port => {
      val client = new MongoClient(host,port)
      client.getDatabase(dbName)
    }
  }

  val mongoClient = new MongoClient(host, port)
  val mongoDatabase = mongoClient.getDatabase(dbName)
}
object IncrementalEndLine{
  def getProductWithEndLine(sourceFile:BufferedSource): List[Int] = {
    var lineNumber = 0
    val buffer: ListBuffer[Int] = new ListBuffer[Int]()
    sourceFile.getLines() foreach { line =>
      if (line.contains("</Product>")) {
        buffer += lineNumber
      }
      lineNumber += 1
    }
    buffer.toList
  }
}
