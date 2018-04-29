package com.sony.utils

import com.typesafe.config.ConfigFactory

object MongoConstant {
  val config = ConfigFactory.load
  val server = config.getString("mongo.url")
  val dbName = config.getString("mongo.dbname")

  val host = "localhost"
  val port = 27017
  val ports = List(27017)
  val numberOfInstances = config.getInt("instances")
}
