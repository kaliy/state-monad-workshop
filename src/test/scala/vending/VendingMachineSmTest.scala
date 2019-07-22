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

      val (newState, (effect1, effect2)) = (for {
        e1 <- VendingMachineSm.updateCredit(Credit(5))
        e2 <- VendingMachineSm.updateCredit(Credit(2))
      } yield (e1, e2)).run(vendingMachineState).value

      newState.credit shouldBe 7
      effect1 contains CreditInfo(5)
      effect2 contains CreditInfo(7)
    }

    "clear credit when withdrawn is selected" in {
      //Copy base state, set credit to 5
      val state0 = vendingMachineState.copy(credit = 5)

      val (newState, effect) = VendingMachineSm.updateCredit(Withdrawn).run(state0).value

      newState.credit shouldBe 0
      effect contains CollectYourMoney
    }

  }

  "select product monad" should {
    "successfully buy product" in {
      val state0 = vendingMachineState.copy(credit = 10)

      val (newState, effect) = VendingMachineSm.selectProduct(SelectProduct(beer.code)).run(state0).value

      newState.credit shouldBe 0
      newState.quantity(beer) shouldBe 4
      effect contains GiveProductAndChange(beer, 7)
    }
    "refuse to buy if not enough of money" in {
      val state0 = vendingMachineState.copy(credit = 2)

      val (newState, effect) = VendingMachineSm.selectProduct(SelectProduct(beer.code)).run(state0).value

      newState.credit shouldBe 2
      newState.quantity(beer) shouldBe 5
      effect contains NotEnoughOfCredit(1)
    }
  }

  "detect shortage monad" should {
    "detect shortage" in {
      val state0 = vendingMachineState.copy(quantity = Map(beer -> 0))

      val (newState, effect) = VendingMachineSm.detectShortage().run(state0).value

      newState.reportedShortage shouldBe Set(beer)
      effect shouldBe List(ProductShortage(beer))
    }
    "ignore shortage for a second time" in {
      val state0 = vendingMachineState.copy(quantity = Map(beer -> 0))

      val (_, (effect1, effect2)) = (for {
        e1 <- VendingMachineSm.detectShortage()
        e2 <- VendingMachineSm.detectShortage()
      } yield (e1, e2)
        ).run(state0).value

      effect1 shouldBe List(ProductShortage(beer))
      effect2 shouldBe List.empty
    }
  }

  "expiry date monad" should {
    val date = beer.expiryDate.plusDays(1)

    "find expired products" in {
      val (newState, effect) = VendingMachineSm.checkExpiryDate(CheckExpiryDate, date).run(vendingMachineState).value

      newState.reportedExpiryDate shouldBe Set(beer, pizza)
      effect contains ExpiredProducts(List(beer, pizza))
    }

    "ignore expired products if already reported" in {
      val (_, effect) = (for {
        _ <- VendingMachineSm.checkExpiryDate(CheckExpiryDate, date)
        e <- VendingMachineSm.checkExpiryDate(CheckExpiryDate, date)
      } yield e).run(vendingMachineState).value

      effect shouldBe empty
    }

    "check that all products are ok" in {
      val (newState, effect) = VendingMachineSm.checkExpiryDate(CheckExpiryDate, LocalDate.MIN).run(vendingMachineState).value

      newState.reportedExpiryDate shouldBe Set()
      effect shouldBe empty
    }
  }

  "detect money box almost full monad" should {
    "notify if money box is almost full" in {
      val state0 = vendingMachineState.copy(income = 11)

      val (_, effect) = VendingMachineSm.detectMoneyBoxAlmostFull().run(state0).value

      effect contains MoneyBoxAlmostFull(11)
    }
  }

  "Vending machine" should {

    "successfully buy and give change" in {
      val (newState, effect) = (for {
        _ <- VendingMachineSm.compose(Credit(10), now)
        e <- VendingMachineSm.compose(SelectProduct(beer.code), now)
      } yield e).run(vendingMachineState).value

      effect.userOutputs should contain(GiveProductAndChange(beer, 7))
      newState.quantity(beer) shouldBe 4
    }

    "refuse to buy if not enough of money" in {
      val (newState, effect) = (for {
        _ <- VendingMachineSm.compose(Credit(2), now)
        e <- VendingMachineSm.compose(SelectProduct(beer.code), now)
      } yield e).run(vendingMachineState).value

      effect.userOutputs should contain(NotEnoughOfCredit(1))
      newState.quantity(beer) shouldBe 5
    }

    "refuse to buy for wrong product selection" in {
      val (_, effect) = VendingMachineSm.compose(SelectProduct("?"), now).run(vendingMachineState).value

      effect.userOutputs should contain(WrongProduct)
    }

    "refuse to buy if out of stock" in {
      val (newState, effect) = (for {
        _ <- VendingMachineSm.compose(Credit(5), now)
        e <- VendingMachineSm.compose(SelectProduct(beer.code), now)
      } yield e).run(vendingMachineState.copy(quantity = Map(beer -> 0))).value

      effect.userOutputs should contain(OutOfStock(beer))
      newState.income shouldBe 0
    }

    "track income" in {
      val (newState, _) = (for {
        _ <- VendingMachineSm.compose(Credit(10), now)
        _ <- VendingMachineSm.compose(SelectProduct(beer.code), now)
        _ <- VendingMachineSm.compose(Credit(10), now)
        _ <- VendingMachineSm.compose(SelectProduct(beer.code), now)
      } yield ()).run(vendingMachineState).value

      newState.income shouldBe 6
      newState.quantity(beer) shouldBe 3
    }

    "track credit" in {
      val (newState, _) = (for {
        _ <- VendingMachineSm.compose(Credit(10), now)
        _ <- VendingMachineSm.compose(Credit(10), now)
      } yield ()).run(vendingMachineState).value

      newState.credit shouldBe 20
    }

    "give back all money if withdraw" in {
      val state0 = vendingMachineState.copy(credit = 10)

      val (newState, _) = VendingMachineSm.compose(Withdrawn, now).run(state0).value

      newState.credit shouldBe 0
    }

    "report if money box is almost full" in {
      val state0 = vendingMachineState.copy(income = 10)

      val (_, effect) = (for {
        _ <- VendingMachineSm.compose(Credit(10), now)
        e <- VendingMachineSm.compose(SelectProduct(beer.code), now)
      } yield e).run(state0).value

      effect.systemReports should contain(MoneyBoxAlmostFull(13))
    }

    "detect shortage of product" in {
      val state0 = vendingMachineState.copy(quantity = Map(beer -> 1))

      val (_, effect) = (for {
        _ <- VendingMachineSm.compose(Credit(10), now)
        e <- VendingMachineSm.compose(SelectProduct(beer.code), now)
      } yield e).run(state0).value

      effect.systemReports should contain(ProductShortage(beer))
    }

    "report issues with expiry date" in {
      val date = beer.expiryDate.plusDays(1)

      val (_, effect) = VendingMachineSm.compose(CheckExpiryDate, date).run(vendingMachineState).value

      effect.systemReports should contain(ExpiredProducts(List(beer, pizza)))
    }

  }

}
