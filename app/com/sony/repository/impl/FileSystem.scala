package com.sony.repository.impl

import akka.actor.ActorRef
import akka.pattern.ask
import com.sony.repository.{SourceService, StorageSystem}
import com.sony.services.StartupCommand
import org.json4s.JValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object FileSystem extends StorageSystem {
  override def loadData(source: SourceService, func: String => JValue, ref: ActorRef): Future[Boolean] = {
    for {
      result <- source.getResult
      res <- ask(ref, StartupCommand(result, func))(5.seconds).mapTo[Future[Boolean]].flatMap(identity)
    } yield res
  }
}

object DbSystem extends StorageSystem {
  override def loadData(source: SourceService, func: String => JValue, ref: ActorRef): Future[Boolean] = {
    for {
      result <- source.getResult
      res <- ask(ref, StartupCommand(result, func))(5.seconds).mapTo[Future[Boolean]].flatMap(identity)
    } yield res
  }
}

//object Sdd extends App{
//  val number = 123.34
//  val strNum = number.toString()
//  println(strNum)
//  val result = if(strNum.contains(".")){
//    val splittedNum = strNum.split('.').toList.last
//    val size = splittedNum.length
//    println(size)
//    val multiplyFactor = Math.pow(10.0 , size)
//    println(multiplyFactor)
//    number * multiplyFactor
//  }
//  else{
//    number
//  }
//  println(result.toInt)
//}