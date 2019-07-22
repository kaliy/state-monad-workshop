package vending

import java.time.LocalDate

import org.scalatest.{Matchers, WordSpec}
import vending.Domain._
import vending.VendingMachineSm.VendingMachineState

class VendingMachineSmTest extends WordSpec with Matchers {

  val now: LocalDate = LocalDate.of(2018, 10, 1)
  private val beer = Product(3, "1", Symbols.beer, LocalDate.of(2020, 12, 10))
  private val pizza = Product(100, "2", Symbols.pizza, LocalDate.of(2018, 12, 10))

  //base state to use in tests
  var vendingMachineState = VendingMachineState(
    credit = 0, income = 0,
    quantity = Map(
      beer -> 5,
      pizza -> 1
    )
  )


  "update credit monad" should {

    "update credit when insert" in {
      //Can use base state without changes
      fail("not implemented")
    }

    "clear credit when withdrawn is selected" in {
      //Copy base state, set credit to 5
      fail("not implemented")
    }

  }

  "select product monad" should {
    "successfully buy product" in {
      fail("not implemented")
    }
    "refuse to buy if not enough of money" in {
      fail("not implemented")
    }
  }

  "detect shortage monad" should {
    "detect shortage" in {
      fail("not implemented")
    }
    "ignore shortage for a second time" in {
      fail("not implemented")
    }
  }

  "expiry date monad" should {

    "find expired products" in {
      fail("not implemented")
    }

    "ignore expired products if already reported" in {
      fail("not implemented")
    }

    "check that all products are ok" in {
      fail("not implemented")
    }
  }

  "detect money box almost full monad" should {
    "notify if money box is almost full" in {
      fail("not implemented")
    }
  }

  "Vending machine" should {

    "successfully buy and give change" in {
      fail("not implemented")
    }

    "refuse to buy if not enough of money" in {
      fail("not implemented")
    }

    "refuse to buy for wrong product selection" in {
      fail("not implemented")
    }

    "refuse to buy if out of stock" in {
      fail("not implemented")
    }

    "track income" in {
      fail("not implemented")
    }

    "track credit" in {
      fail("not implemented")
    }

    "give back all money if withdraw" in {
      fail("not implemented")
    }

    "report if money box is almost full" in {
      fail("not implemented")
    }

    "detect shortage of product" in {
      fail("not implemented")
    }

    "report issues with expiry date" in {
      fail("not implemented")
    }

  }

}
