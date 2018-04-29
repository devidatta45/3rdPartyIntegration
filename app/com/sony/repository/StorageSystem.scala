package com.sony.repository

import akka.actor.ActorRef
import org.json4s.JValue

import scala.concurrent.Future

trait StorageSystem{
  def loadData(source:SourceService,func: String => JValue,ref:ActorRef): Future[Boolean]
}
trait SourceService {
  def getResult: Future[List[String]]
}