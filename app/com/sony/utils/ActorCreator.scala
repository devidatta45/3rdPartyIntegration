package com.sony.utils

import javax.inject.{Inject, Singleton}

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by DDM on 24-04-2017.
  */
@Singleton
class ActorCreator @Inject()(system: ActorSystem) {
  implicit val timeout: Timeout = 5.seconds
  val ref = system.actorOf(Props[ActorSupervisor])

  def createRouter(props: Props, name: String): ActorRef = {
    val future = ask(ref, ActorName(props, name)).mapTo[ActorRef]
    Await.result(future, 5.seconds)
  }

  def createActorRef(props: Props, name: String): ActorRef = {
    createRouter(RoundRobinPool(1).props(props), name)
  }
}

class ActorSupervisor extends Actor {
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case cmd: Throwable => {
        println(s"Exception occured: ${cmd.printStackTrace()}")
        Resume
      }
    }

  override def receive: Receive = {
    case cmd: ActorName => sender ! context.actorOf(cmd.props, cmd.name)
  }
}

case class ActorName(props: Props, name: String)

trait BaseActor extends Actor {

  def normalExecution: Receive

  override def receive: Receive = normalExecution orElse handleError

  def handleError: Receive = {
    case cmd: Exception => {
      throw cmd
    }
  }
}
