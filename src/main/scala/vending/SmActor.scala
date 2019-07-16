package vending

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.{Actor, ActorRef}
import vending.Domain._
import vending.VendingMachineSm.VendingMachineState

class SmActor(quantity: Map[Product, Int],
              userReportActor: ActorRef,
              reportsActor: ActorRef)
  extends Actor {

  var vendingMachineState = VendingMachineState(
    credit = 0,
    income = 0,
    quantity = quantity
  )

  override def preStart(): Unit = {
    super.preStart()
    context.system.scheduler
      .schedule(5 seconds, 5 seconds, self, CheckExpiryDate)(context.system.dispatcher)
  }

  override def receive: Receive = {
    case a: Action =>
      //Build logic with VendingMachineSm
      //run logic with current state and capture new state + result
      //update state
      //execute side effects by sending messages
      // systemReports to reportsActor
      // userOutputs to userReportActor

    case GetState => sender() ! vendingMachineState
  }
}
