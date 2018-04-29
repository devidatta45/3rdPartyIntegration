package com.sony.utils

import com.typesafe.config.{ConfigFactory, ConfigObject}
import scala.collection.JavaConverters._

object ChangeConstant {
  val config = ConfigFactory.load()

  val list: Iterable[ConfigObject] = config.getObjectList("change").asScala

  val pairs = for {
    item: ConfigObject <- list
    entry <- item.entrySet().asScala
    key = entry.getKey
    value = entry.getValue.atKey(key)
  } yield (key, value.getString(key))

  def getMappings = pairs.toMap
}