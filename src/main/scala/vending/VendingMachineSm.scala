package vending

import java.time.LocalDate

import cats.data.State
import cats.syntax.option._
import cats.syntax.show._
import vending.Domain._

object VendingMachineSm {

  //    State monad is function
  // ╭----------------------------╮
  // |  State => (State, Effect)  |
  // ╰----------------------------╯
  //
  // In our case
  // Vending machine state => (New vending machine state, effects)

  //1. Update credit when coin is inserted or withdrawn button pressed
  def updateCredit(action: Action) = State[VendingMachineState, Option[UserOutput]] { state =>
    action match {
      case Credit(value) =>
        val newState = state.copy(credit = state.credit + value)
        (newState, CreditInfo(newState.credit).some)
      case Withdrawn =>
        val newState = state.copy(credit = 0)
        (newState, CollectYourMoney.some)
      case _ => (state, None)
    }
  }

  //2. Handle selection
  def selectProduct(action: Action) = State[VendingMachineState, Option[UserOutput]] { state =>
    action match {
      case SelectProduct(number) =>
        val selected = number.toString
        val maybeProduct = state.quantity.keys.find(_.code == selected)
        val maybeQuantity = maybeProduct.map(state.quantity)
        (maybeProduct, maybeQuantity) match {
          case (Some(product), Some(q)) if product.price <= state.credit && q > 0 =>
            val giveChange = state.credit - product.price
            val newQuantity = q - 1

            val newState = state.copy(
              credit = 0,
              income = state.income + product.price,
              quantity = state.quantity.updated(product, newQuantity)
            )

            (newState, GiveProductAndChange(product, giveChange).some)
          case (Some(product), Some(q)) if q < 1 =>
            (state, OutOfStock(product).some)

          case (Some(product), _) =>
            (state, NotEnoughOfCredit(product.price - state.credit).some)

          case (None, _) =>
            (state, WrongProduct.some)
        }
      case _ => (state, None)
    }
  }

  //3. Detect shortage and notify owner
  def detectShortage() = State[VendingMachineState, List[ProductShortage]] { state =>
    val toNotify = state.quantity.filter(_._2 == 0).keySet -- state.reportedShortage
    if (toNotify.isEmpty) (state, List.empty)
    else (state.copy(reportedShortage = state.reportedShortage ++ toNotify), toNotify.map(ProductShortage).toList)
  }

  //4. Detect that machine has a lot of cash and notify owner
  def detectIfMoneyBoxIsAlmostFull() = State[VendingMachineState, Option[MoneyBoxAlmostFull]] { state =>
    //TODO: implement storing notification state here and in a regular actor
    if (state.income > 10) (state, MoneyBoxAlmostFull(state.income).some)
    else (state, None)
  }

  //5. After user action decide if UI have to refreshed with new state
  def maybeDisplay(action: Action) = State[VendingMachineState, Option[Display]] { state =>
    action match {
      case SelectProduct(_) | Withdrawn | Credit(_) => (state, Display(state.show).some)
      case _ => (state, None)
    }
  }

  //6. Check expiry date of products
  def checkExpirationDate(action: Action, now: LocalDate) = State[VendingMachineState, Option[SystemReporting]] { state =>
    action match {
      case CheckExpiryDate =>
        val expiredProducts = state.quantity.keys.filter { p =>
          !p.expiryDate.isAfter(now) && !state.reportedExpiryDate.contains(p)
        }
        if (expiredProducts.nonEmpty) (
          state.copy(reportedExpiryDate = state.reportedExpiryDate ++ expiredProducts.toSet),
          ExpiredProducts(expiredProducts.toList).some
        ) else (state, None)

      case _ => (state, None)
    }

  }


  def compose(action: Action, now: LocalDate): State[VendingMachineState, ActionResult] =
    for {
      updateCredit <- updateCredit(action)
      selectProduct <- selectProduct(action)
      maybeDisplay <- maybeDisplay(action)
      detectShortage <- detectShortage()
      mbaf <- detectIfMoneyBoxIsAlmostFull()
      expiration <- checkExpirationDate(action, now)
    } yield ActionResult(
      userOutputs = List(updateCredit, selectProduct, maybeDisplay).flatten,
      systemReports = mbaf.toList ::: detectShortage ::: expiration.toList
    )

  case class VendingMachineState(credit: Int,
                                 income: Int,
                                 quantity: Map[Product, Int] = Map.empty,
                                 reportedExpiryDate: Set[Domain.Product] = Set.empty[Domain.Product],
                                 reportedShortage: Set[Domain.Product] = Set.empty[Domain.Product]
                                )

}
