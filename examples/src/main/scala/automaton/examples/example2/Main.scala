package automaton.examples.example2

import automaton.*
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.*

case class ActionService(gitHub: GitHub, discord: Discord):
  def getMyRepos: Task[List[Repository]] =
    gitHub.myRepos

  def sendDiscordMessage(message: String): Task[Unit] =
    discord.sendMessage(message)

object ActionService:
  val layer = ZLayer.fromFunction(ActionService.apply _)

object Main extends ZIOAppDefault:
  val run =
    ChatService
      .prompt[ActionService](
        "Send a list of my repos (only those that mention macros) to Discord"
      )
      .provide(
        ChatService.live[ActionService],
        ActionService.layer,
        GitHubLive.layer,
        DiscordLive.layer,
        HttpClientZioBackend.layer()
      )
