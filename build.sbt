name := "state-monad-workshop"
version := "1.0"

scalaVersion := "2.12.8"

scalacOptions ++= Seq(
  "-encoding", "UTF-8",   // source files are in UTF-8
  "-deprecation",         // warn about use of deprecated APIs
  "-unchecked",           // warn about unchecked type parameters
  "-feature",             // warn about misused language features
  "-language:higherKinds",// allow higher kinded types without `import scala.language.higherKinds`
  "-Xlint",               // enable handy linter warnings
  "-Xfatal-warnings",     // turn compiler warnings into errors
  "-Ypartial-unification" // allow the compiler to unify type constructors of different arities
)

testOptions in Test ++= Seq(Tests.Argument(TestFrameworks.ScalaTest, "-o"), Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports"))

libraryDependencies += "org.typelevel" %% "cats-core" % "1.6.1"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.23",
  "com.typesafe.akka" %% "akka-persistence" % "2.5.23",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.23" % Test
)

libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
libraryDependencies += "org.iq80.leveldb" % "leveldb" % "0.12"

//Memory persistence for testing
resolvers += "dnvriend" at "http://dl.bintray.com/dnvriend/maven"
libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.15.2" % Test

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.8" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
libraryDependencies += "org.pegdown"    %  "pegdown"     % "1.6.0"  % Test


addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.10")

