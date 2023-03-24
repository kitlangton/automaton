package automaton.examples.example2

import zio.*
import zio.json.*
import sttp.client3.*
import sttp.client3.httpclient.zio.{HttpClientZioBackend, SttpClient}
import sttp.client3.ziojson.*

trait GitHub:
  def myRepos: Task[List[Repository]]

final case class Repository(
    id: Long,
    name: String,
    url: String,
    description: Option[String],
    stargazers_count: Int
) derives JsonDecoder:
  override def toString: String =
    s"Repository(name = $name, url = $url, description = $description, stars = $stargazers_count)"

final case class GitHubLive(client: SttpClient) extends GitHub:
  def myRepos: Task[List[Repository]] =
    basicRequest
      .get(uri"https://api.github.com/users/kitlangton/repos?sort=updated")
      .response(asJson[List[Repository]])
      .send(client)
      .flatMap { resp =>
        ZIO.fromEither(resp.body)
      }

object GitHubLive:
  val layer = ZLayer.fromFunction(GitHubLive.apply _)

object GithubDemo extends ZIOAppDefault:
  val program = ZIO.serviceWithZIO[GitHub](_.myRepos.debug)

  val run = program.provide(
    GitHubLive.layer,
    HttpClientZioBackend.layer()
  )
