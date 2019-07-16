package vending

import java.time.LocalDate

import cats.data.State
//import cats.syntax.option._
//import cats.syntax.show._
import vending.Domain._

object VendingMachineSm {

  //    State monad is function
  // ╭----------------------------╮
  // |  State => (State, Effect)  |
  // ╰----------------------------╯
  //
  // In our case
  // Vending machine state => (New vending machine state, effects)

  //write functions to

  def compose(action: Action, now: LocalDate): State[VendingMachineState, ActionResult] = ???
  // use for comprehension to compose logic from smaller parts



  case class VendingMachineState(credit: Int,
                                 income: Int,
                                 quantity: Map[Product, Int] = Map.empty,
                                 reportedExpiryDate: Set[Domain.Product] = Set.empty[Domain.Product],
                                 reportedShortage: Set[Domain.Product] = Set.empty[Domain.Product]
                                )

}
