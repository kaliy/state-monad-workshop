#  Using Functional State in Actors
This project is used for workshop [Scala Summer Camp #1: Using Functional State in Actors](https://www.meetup.com/pl-PL/Krakow-Scala-User-Group/events/263171988/). Slides for workshop are [here](https://docs.google.com/presentation/d/145eJgOup8CKicd7B7GFTZiUvsYvu1icMvGz45Sk5ttk/edit?usp=sharing)

Requirements:
- git
- Java 8+,
- sbt ([Installation manual](https://www.scala-sbt.org/1.x/docs/Setup.html))

Instructions:
1. Clone project
2. Run `sbt test`, tests will fail
3. run `sbt "runMain vending.VendingMachineDemo"` to start demo application
4. Implement logic and tests



## What is state monad

State is a structure that provides a functional approach to handling application state. State[S, A] is basically a function 
```scala
S => (S, A)
```
where S is the type that represents your state and A is the result the function produces. In addition to returning the result of type A, the function returns a new S value, which is the updated state. ([Cats docs](https://typelevel.org/cats/datatypes/state.html#state))


State monad is a monad so we can chain them using for comprehension:

```scala
val monad123 = for {
 a1 <- monad1
 a2 <- monad2
 a3 <- monad3
} yield Result(a1, a2, a3)

```

`monad123` is type of `State[S, Result]`. It has to be run with specific state to get result:

```scala
val myState = MyState("A")
val (newMyState, result) = monad123.run(myState).value
```