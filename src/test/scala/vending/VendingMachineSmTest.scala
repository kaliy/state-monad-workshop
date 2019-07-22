package vending

import java.time.LocalDate

import org.scalatest.{Matchers, WordSpec}
import vending.Domain._
import vending.VendingMachineSm._

class VendingMachineSmTest extends WordSpec with Matchers {

  val now: LocalDate = LocalDate.of(2018, 10, 1)
  private val beer = Product(3, "1", Symbols.beer, LocalDate.of(2020, 12, 10))
  private val pizza = Product(100, "2", Symbols.pizza, LocalDate.of(2018, 12, 10))

  var vendingMachineState = VendingMachineState(
    credit = 0, income = 0,
    quantity = Map(
      beer -> 5,
      pizza -> 1
    )
  )

  "update credit monad" should {

    "update credit when insert" in {
      val (newState, (effect1, effect2)) = (for {
        e1 <- updateCredit(Credit(666))
        e2 <- updateCredit(Credit(34))
      } yield (e1, e2)).run(vendingMachineState).value

      newState.credit shouldBe 700
      effect1 should contain (CreditInfo(666))
      effect2 should contain (CreditInfo(700))
    }

    "clear credit when withdrawn is selected" in {
      val state = vendingMachineState.copy(credit = 666)
      val (newState, effect) = updateCredit(Withdrawn).run(state).value

      newState.credit shouldBe 0
      effect should contain (CollectYourMoney)
    }

  }

  "select product monad" should {
    "successfully buy product" in {
      val state = vendingMachineState.copy(credit = 20)
      val (newState, effect) = selectProduct(SelectProduct(beer.code)).run(state).value

      newState.credit shouldBe 0
      newState.quantity should contain (beer -> 4)
      newState.income shouldBe 3
      effect should contain (GiveProductAndChange(beer, 17))
    }

    "refuse to buy if not enough of money" in {
      val state = vendingMachineState.copy(credit = 1)
      val (newState, effect) = selectProduct(SelectProduct(beer.code)).run(state).value

      newState shouldBe state
      effect should contain (NotEnoughOfCredit(2))
    }

    "report incorrect selection" in {
      val state = vendingMachineState.copy(credit = 10)
      val (newState, effect) = selectProduct(SelectProduct("sasiska")).run(state).value

      newState shouldBe state
      effect should contain (WrongProduct)
    }

    "report that item is out of stock" in {
      val state = vendingMachineState.copy(credit = 10, quantity = vendingMachineState.quantity.updated(beer, 0))
      val (newState, effect) = selectProduct(SelectProduct(beer.code)).run(state).value

      newState shouldBe state
      effect should contain (OutOfStock(beer))
    }
  }

  "detect shortage monad" should {
    "detect shortage" in {
      val state = vendingMachineState.copy(
        quantity = vendingMachineState.quantity.updated(beer, 0).updated(pizza, 0)
      )

      val (newState, effect) = detectShortage().run(state).value

      newState.reportedShortage should contain only (beer, pizza)
      effect should contain only (ProductShortage(beer), ProductShortage(pizza))
    }
    "ignore shortage for a second time" in {
      val state = vendingMachineState.copy(
        quantity = vendingMachineState.quantity.updated(beer, 0).updated(pizza, 0)
      )

      val (_, effect) = (for {
        _ <- detectShortage()
        e2 <- detectShortage()
      } yield e2).run(state).value

      effect shouldBe empty
    }
  }

  "expiry date monad" should {
    "find expired products" in {
      val (newState, effect) = checkExpirationDate(CheckExpiryDate, LocalDate.MAX).run(vendingMachineState).value

      newState.reportedExpiryDate should contain only (beer, pizza)

      effect should contain (ExpiredProducts(List(beer, pizza)))
    }

    "ignore expired products if already reported" in {
      val (state, e) = (for {
        _ <- checkExpirationDate(CheckExpiryDate, LocalDate.MAX)
        e <- checkExpirationDate(CheckExpiryDate, LocalDate.MAX)
      } yield e).run(vendingMachineState).value

      state.reportedExpiryDate should contain only (beer, pizza)

      e shouldBe empty
    }

    "check that all products are ok" in {
      val (newState, effect) = checkExpirationDate(CheckExpiryDate, LocalDate.MIN).run(vendingMachineState).value

      newState.reportedExpiryDate shouldBe empty
      effect shouldBe empty
    }
  }

  "detect money box almost full monad" should {
    "notify if money box is almost full" in {
      val state = vendingMachineState.copy(income = 100)

      val (newState, effect) = detectIfMoneyBoxIsAlmostFull().run(state).value

      newState shouldBe state
      effect should contain (MoneyBoxAlmostFull(100))
    }
  }

  "Vending machine" should {

    "successfully buy and give change" in {
      val (state, (e1, e2)) = (for {
        e1 <- compose(Credit(5), now)
        e2 <- compose(SelectProduct(beer.code), now)
      } yield (e1, e2)).run(vendingMachineState).value

      state.credit shouldBe 0
      state.income shouldBe 3
      state.quantity should contain (beer -> 4)

      e1.userOutputs should contain (CreditInfo(5))
      e2.userOutputs should contain (GiveProductAndChange(beer, 2))
    }

    "refuse to buy if not enough of money" in {
      val newState = vendingMachineState.copy(credit = 2)

      val (state, effect) = compose(SelectProduct(beer.code), now).run(newState).value

      state shouldBe newState

      effect.userOutputs should contain (NotEnoughOfCredit(1))
    }

    "refuse to buy for wrong product selection" in {
      val newState = vendingMachineState.copy(credit = 5)

      val (state, effect) = compose(SelectProduct("sasiska"), now).run(newState).value

      state shouldBe newState

      effect.userOutputs should contain (WrongProduct)
    }

    "refuse to buy if out of stock" in {
      val newState = vendingMachineState.copy(
        quantity = vendingMachineState.quantity.updated(beer, 0),
        credit = 5
      )

      val (state, effect) = compose(SelectProduct(beer.code), now).run(newState).value

      state.credit shouldBe 5
      state.income shouldBe 0

      effect.userOutputs should contain (OutOfStock(beer))
    }

    "track income" in {
      val (state, (e2, e4)) = (for {
        _ <- compose(Credit(5), now)
        e2 <- compose(SelectProduct(beer.code), now)
        _ <- compose(Credit(5), now)
        e4 <- compose(SelectProduct(beer.code), now)
      } yield (e2, e4)).run(vendingMachineState).value

      state.income shouldBe 6
    }

    "track credit" in {
      val (state, (e1, e2)) = (for {
        e1 <- compose(Credit(5), now)
        e2 <- compose(Credit(10), now)
      } yield (e1, e2)).run(vendingMachineState).value

      state.credit shouldBe 15

      e1.userOutputs should contain (CreditInfo(5))
      e2.userOutputs should contain (CreditInfo(15))
    }

    "give back all money if withdraw" in {
      val newState = vendingMachineState.copy(credit = 666)

      val (state, effect) = compose(Withdrawn, now).run(newState).value

      state.credit shouldBe 0

      effect.userOutputs should contain (CollectYourMoney)
    }

    "report if money box is almost full" in {
      val newState = vendingMachineState.copy(income = 666)

      val (state, effect) = compose(Withdrawn, now).run(newState).value

      effect.systemReports should contain (MoneyBoxAlmostFull(666))
    }

    "detect shortage of product" in {
      val newState = vendingMachineState.copy(credit = 5, quantity = vendingMachineState.quantity.updated(beer, 0))

      val (state, effect) = compose(Withdrawn, now).run(newState).value

      effect.systemReports should contain (ProductShortage(beer))
    }

    "report issues with expiry date" in {
      val oldPizza = pizza.copy(expiryDate = LocalDate.MIN)
      val newState = vendingMachineState.copy(quantity = vendingMachineState.quantity.updated(oldPizza, 0))

      val (state, effect) = compose(CheckExpiryDate, now).run(newState).value

      effect.systemReports should contain (ExpiredProducts(List(oldPizza)))
    }

  }

}
