package com.sony.controllers

import akka.actor.Props
import akka.util.Timeout
import com.google.inject.{Inject, Singleton}
import com.sony.services.{BVApiService, RouterActor, XmlApiService}
import com.sony.utils.ActorCreator
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask
import com.sony.repository.{SourceService, StorageSystem}
import com.sony.repository.impl.{DbSystem, FileSystem}

@Singleton
class ProductController @Inject()(creator: ActorCreator, cc: ControllerComponents) extends AbstractController(cc) {
  implicit val timeout: Timeout = 5.seconds
  val routerActor = creator.createActorRef(Props(classOf[RouterActor]), "ProductActor")

  val storeFileXml = Action.async {
    val request =  SourceSink(XmlApiService,FileSystem)
    val result = ask(routerActor, request).mapTo[Future[Boolean]]
    val actualResult = result.flatMap(identity)
    actualResult map { res =>
      if (res) {
        Ok("Loaded successfully to file")
      }
      else {
        InternalServerError("Not Loaded")
      }
    }
  }
  val storeFileApi = Action.async {
    val request =  SourceSink(BVApiService,FileSystem)
    val result = ask(routerActor, request).mapTo[Future[Boolean]]
    val actualResult = result.flatMap(identity)
    actualResult map { res =>
      if (res) {
        Ok("Loaded successfully to file")
      }
      else {
        InternalServerError("Not Loaded")
      }
    }
  }

  val storeDbApi = Action.async {
    val request =  SourceSink(BVApiService,DbSystem)
    val result = ask(routerActor, request).mapTo[Future[Boolean]]
    val actualResult = result.flatMap(identity)
    actualResult map { res =>
      if (res) {
        Ok("Loaded successfully to db")
      }
      else {
        InternalServerError("Not Loaded")
      }
    }
  }
  val storeDbXml = Action.async {
    val request =  SourceSink(XmlApiService,DbSystem)
    val result = ask(routerActor, request).mapTo[Future[Boolean]]
    val actualResult = result.flatMap(identity)
    actualResult map { res =>
      if (res) {
        Ok("Loaded successfully to db")
      }
      else {
        InternalServerError("Not Loaded")
      }
    }
  }
}
case class SourceSink(sourceService: SourceService,storageSystem: StorageSystem)