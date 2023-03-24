package automaton

import zio.{UIO, ZIO}

object Log:
  def system(message: String): UIO[Unit] =
    ZIO.debug(scala.Console.YELLOW + message + scala.Console.RESET)

  def systemLine(message: String): UIO[Unit] =
    ZIO.succeed(print(scala.Console.YELLOW + message + scala.Console.RESET))

  def assistant(message: String): UIO[Unit] =
    ZIO.debug(scala.Console.BLUE + message + scala.Console.RESET)

  def user(message: String): UIO[Unit] =
    ZIO.debug(scala.Console.GREEN + message + scala.Console.RESET)
