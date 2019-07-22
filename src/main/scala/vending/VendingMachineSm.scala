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

  //TODO write small functions to update handle specific action
  //test cases for these functions are defined in VendingMachineSmTest

  //1. Update credit when coin is inserted or withdrawn button pressed
  def updateCredit(action: Action) = State[VendingMachineState, Option[UserOutput]] { state =>
    action match {
      case Credit(value) =>
        val newState = state.copy(credit = state.credit + value)
        (newState, CreditInfo(newState.credit).some)
      case Withdrawn =>
        (state.copy(credit = 0), CollectYourMoney.some)
      case _ => (state, none[UserOutput])
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
            val giveChange = state.credit - product.price // calculating new state
          val newQuantity = q - 1 // .....................
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
      case _ => (state, none[UserOutput])
    }
  }

  //3. Detect shortage and notify owner
  def detectShortage() = State[VendingMachineState, List[ProductShortage]] { state =>
    val toNotify: Set[Product] = state.quantity.filter(_._2 == 0).keySet -- state.reportedShortage
    if (toNotify.isEmpty) {
      (state, List.empty)
    } else {
      val newState = state.copy(reportedShortage = state.reportedShortage ++ toNotify)
      (newState, toNotify.toList.map(ProductShortage))
    }
  }

  //4. Detect that machine has a lot of cash and notify owner
  def detectMoneyBoxAlmostFull() = State[VendingMachineState, Option[MoneyBoxAlmostFull]] { state =>
    if (state.income > 10) {
      (state, MoneyBoxAlmostFull(state.income).some)
    } else {
      (state, none[MoneyBoxAlmostFull])
    }

  }

  //5. After user action decide if UI have to refreshed with new state
  def maybeDisplayState(action: Action) = State[VendingMachineState, Option[Display]] { state =>
    action match {
      case Credit(_) | Withdrawn | SelectProduct(_) => (state, Display(state.show).some)
      case _ => (state, none[Display])
    }
  }

  //6. Check expiry date of products
  def checkExpiryDate(action: Action, now: LocalDate) = State[VendingMachineState, Option[ExpiredProducts]] { state =>
    if (action == CheckExpiryDate){
      val products = state.quantity.keys.filter(p => !p.expiryDate.isAfter(now) && !state.reportedExpiryDate.contains(p))
      val newState = state.copy(reportedExpiryDate = state.reportedExpiryDate ++ products)
      if (products.isEmpty) {
        (newState, none[ExpiredProducts])
      } else {
        (newState, ExpiredProducts(products.toList).some)
      }
    } else {
      (state, none[ExpiredProducts])
    }
  }


  def compose(action: Action, now: LocalDate): State[VendingMachineState, ActionResult] =
    for {
      updateCreditResult <- updateCredit(action)
      selectionResult <- selectProduct(action)
      maybeShortage <- detectShortage()
      expiredResult <- checkExpiryDate(action, now)
      maybeMbaf <- detectMoneyBoxAlmostFull()
      maybeDisplay <- maybeDisplayState(action)
    } yield ActionResult(
      userOutputs = List(updateCreditResult,selectionResult,maybeDisplay).flatten,
      systemReports = List(expiredResult, maybeMbaf).flatten ++ maybeShortage
    )


  case class VendingMachineState(credit: Int,
                                 income: Int,
                                 quantity: Map[Product, Int] = Map.empty,
                                 reportedExpiryDate: Set[Domain.Product] = Set.empty[Domain.Product],
                                 reportedShortage: Set[Domain.Product] = Set.empty[Domain.Product]
                                )

}
