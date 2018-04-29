package com.sony.services

import com.google.inject.{Inject, Singleton}
import com.sony.controllers.SourceSink
import com.sony.repository.impl.{DbSystem, FileSystem}
import com.sony.utils.BaseActor
import org.json4s.JValue
import org.json4s.Xml.toJson
import org.json4s.jackson.JsonMethods._

import scala.xml.Node

@Singleton
class RouterActor @Inject()(actors: DefaultActors) extends BaseActor {
  override def normalExecution: Receive = {
    case cmd: SourceSink => {
      cmd.sourceService match {
        case BVApiService => {
          cmd.storageSystem match {
            case DbSystem => sender ! cmd.storageSystem.loadData(cmd.sourceService, jsonChange, actors.parallelRef)
            case FileSystem => sender ! cmd.storageSystem.loadData(cmd.sourceService, jsonChange, actors.fileParallelRef)
          }
        }

        case XmlApiService => {
          cmd.storageSystem match {
            case DbSystem => sender ! cmd.storageSystem.loadData(cmd.sourceService, xmlChange, actors.parallelRef)
            case FileSystem => sender ! cmd.storageSystem.loadData(cmd.sourceService, xmlChange, actors.fileParallelRef)
          }
        }
      }
    }
  }

  def xmlChange(res: String): JValue = {
    val newProduct: Node = scala.xml.XML.loadString(res)
    toJson(newProduct)
  }

  def jsonChange(res: String): JValue = parse(res)
}

case class ProductInfo(id: String, passkey: String, locale: String)