package com.sony.services

import akka.actor.Props
import com.google.inject.{Inject, Singleton}
import com.sony.utils.{ActorCreator, BaseActor, IncrementalEndLine, MongoConnectConfig}
import org.bson.types.ObjectId
import org.json4s.JValue
import org.json4s.Xml.toJson

import scala.io.Source
import scala.xml.Node

@Singleton
class MongoParallelActor @Inject() (actors:DefaultActors)extends MongoConnectConfig with BaseActor {
  override def collection: String = "Products"

  override def lookupCollection: String = "ProductLookup"

  override def nativeReviewStatisticsCollection: String = "NativeReviewStatistics"

  override def reviewStatisticsCollection: String = "ReviewStatistics"

  override def reviewsCollection: String = "Reviews"

  override def normalExecution: Receive = {
    case cmd: StartupCommand => {
      //val productList = getProducts()
      cmd.productList.par foreach {
        product => {
          //val newProduct: Node = scala.xml.XML.loadString(product)
          val productId: ObjectId = ObjectId.get()
          insertData(cmd.func(product), productId,actors)
        }
      }
      sender ! true
    }
    case cmd: IncrementalCommand => {
      val incrSourceFile = Source.fromFile(cmd.file)
      val result = IncrementalEndLine.getProductWithEndLine(incrSourceFile)
      val productList = getProducts(result, cmd.file)
      productList.filterNot(_.equals("")).par foreach {
        product => {
          val newProduct: Node = scala.xml.XML.loadString(product)
          updateData(toJson(newProduct),actors)
        }
      }
    }
  }


  override lazy val file: String = "./masterData/bv_sony-global_standard_client_feed.xml"
}

@Singleton
class DefaultActors @Inject()(creator: ActorCreator) {
  val productRef = creator.createActorRef(Props(classOf[ProductActor]), "ProductActor")
  val parallelRef = creator.createActorRef(Props(classOf[MongoParallelActor]), "MongoParallelActor")
  val fileParallelRef = creator.createActorRef(Props(classOf[FileParallelActor]), "FileParallelActor")
  val reviewRef = creator.createActorRef(Props(classOf[ReviewsActor]), "ReviewsActor")
  val productLookupRef = creator.createActorRef(Props(classOf[ProductLookupActor]), "ProductLookupActor")
  val nativeReviewStatisticsRef = creator.createActorRef(Props(classOf[NativeReviewStatisticsActor]), "NativeReviewStatisticsActor")
  val reviewStatisticsRef = creator.createActorRef(Props(classOf[ReviewStatisticsActor]), "ReviewStatisticsActor")
}

case class StartupCommand(productList: List[String], func: String => JValue)

case class IncrementalCommand(file: String)
