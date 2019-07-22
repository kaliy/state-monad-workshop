package vending

import java.time.LocalDate

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
      val (newState, result) = VendingMachineSm.compose(a,LocalDate.now()).run(vendingMachineState).value
      vendingMachineState = newState
      result.userOutputs.foreach(userReportActor ! _)
      result.systemReports.foreach(reportsActor ! _)

    // Used for test
    case GetState => sender() ! vendingMachineState
  }
}
