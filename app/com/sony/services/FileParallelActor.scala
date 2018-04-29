package com.sony.services

import com.google.inject.{Inject, Singleton}
import com.sony.utils.BaseActor

@Singleton
class FileParallelActor @Inject()(actors: DefaultActors) extends BaseActor {
  override def normalExecution: Receive = {
    case cmd: StartupCommand => {
      cmd.productList.par foreach {
        product => {
          val value = cmd.func(product)
        }
      }
      sender ! true
    }
  }
}
