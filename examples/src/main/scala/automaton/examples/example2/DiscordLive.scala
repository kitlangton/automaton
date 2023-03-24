package automaton.examples.example2

import sttp.client3.*
import sttp.client3.httpclient.zio.*
import zio.json.*
import zio.*

trait Discord:
  def sendMessage(message: String): Task[Unit]

final case class DiscordLive(client: SttpClient) extends Discord:
  def sendMessage(message: String): Task[Unit] =
    basicRequest
      .post(
        uri"https://discord.com/api/webhooks/YOUR_WEBHOOK_HERE"
      )
      .header("Content-Type", "application/json")
      .body(Map("content" -> message).toJson)
      .send(client)
      .unit

object DiscordLive:
  val layer = ZLayer.fromFunction(DiscordLive.apply _)

object DiscordDemo extends ZIOAppDefault:

  val program =
    ZIO.serviceWithZIO[Discord](_.sendMessage("Hello World [link](https://scala.school)"))

  val run = program.provide(
    DiscordLive.layer,
    HttpClientZioBackend.layer()
  )
