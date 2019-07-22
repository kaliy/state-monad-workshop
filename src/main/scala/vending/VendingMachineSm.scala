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

  //TODO write small functions to update handle specific action
  //test cases for these functions are defined in VendingMachineSmTest

  //1. Update credit when coin is inserted or withdrawn button pressed

  //2. Handle selection

  //3. Detect shortage and notify owner

  //4. Detect that machine has a lot of cash and notify owner

  //5. After user action decide if UI have to refreshed with new state

  //6. Check expiry date of products


  def compose(action: Action, now: LocalDate): State[VendingMachineState, ActionResult] = ???
  //TODO use for comprehension to compose logic from smaller parts (1-6 above)


  case class VendingMachineState(credit: Int,
                                 income: Int,
                                 quantity: Map[Product, Int] = Map.empty,
                                 reportedExpiryDate: Set[Domain.Product] = Set.empty[Domain.Product],
                                 reportedShortage: Set[Domain.Product] = Set.empty[Domain.Product]
                                )

}
