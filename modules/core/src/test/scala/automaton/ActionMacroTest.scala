package automaton

import zio._
import zio.test._

object ActionMacroTest extends ZIOSpecDefault {
  trait SomeService {
    def successfulCall: ZIO[Any, Nothing, Int] =
      ZIO.succeed(1)

    def failedCall: ZIO[Any, Throwable, String] =
      ZIO.fail(new Exception("No String for you!"))
  }

  val serviceImpl = new SomeService {}
  val handler = ActionMacro.run[SomeService]

  def spec =
    suite("ActionMacro")(
      suite("Handler returned from run")(
        test("happy"){
          for {
            fooResult <-
              handler.call.apply(
                serviceImpl,
                RpcCall("successfulCall", Array.empty)
              )
          } yield assertTrue(fooResult == 1)
        },
        test("sad") {
          for {
            barFailure <-
              handler.call.apply(
                serviceImpl,
                RpcCall("failedCall", Array.empty)
              ).flip
          } yield assertTrue(barFailure.getMessage == "No String for you!")
        }
      ),
      suite("string representation")(
        test("happy"){
           assertTrue(handler.description ==
             """def successfulCall(): Int
               |def failedCall(): String""".stripMargin)
        },
      ),
      suite("quoted tests")(
        test("simple"){
          import scala.quoted.*
          inline def getMethods = ${ActionMacro.getTraitMethods[SomeService]}

          val res = getMethods
          println("=====methods======")
          pprint.pprintln(res)
          println("=====methods======")

          for
            _ <- ZIO.unit
          yield assertCompletes
        }
      )

    )
}
