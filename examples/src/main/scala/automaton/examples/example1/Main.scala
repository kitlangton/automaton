package automaton.examples.example1
import automaton.*
import zio.*

case class ActionService():
  private val cities        = Array("New York", "Space", "Paris", "Tokyo", "Sydney")
  private def randomCity    = cities(scala.util.Random.nextInt(cities.length))
  private val weathers      = Array("extremely hot and humid", "covered in bees", "literally on fire")
  private def randomWeather = weathers(scala.util.Random.nextInt(weathers.length))

  // { "rpc": "addNumbers", "args": [2156, 2366] }
  // { "rpc": "getUserCity" }
  // { "rpc": "getWeather", "args": ["New York"] }
  // { "rpc": "sendSlackMessage", "args": ["The weather in New York is extremely hot and humid."]
//  Map(
//    "addNumbers" -> (a: Int, b: Int) => service.addNumbers(a, b),
//    "getUserCity" -> (service.getUserCity _),
//    "getWeather" -> (city: String) => service.getWeather(city),
//    "sendSlackMessage" -> (markdown: String) => service.sendSlackMessage(markdown)
//  )
  final case class City(name: String)
  // inherently stable
  // inherent stability --->>>>>
  // fly-by-wire == probabilistic
  // subtle, constant course corrections
  // tolerance of errors
  //
  // API -> <- API
  // API -> AI <-> AI <- API
  //
  // City = { "name": "New York" }

  def addNumbers(a: Int, b: Int): Task[Int] =
    ZIO.succeed(a + b).delay(1.second)

  def getUserCity: Task[String] =
    ZIO.succeed(randomCity).delay(1.second)

  def getWeather(city: String): Task[String] =
    ZIO.succeed(randomWeather).delay(1.second)

  def sendSlackMessage(markdown: String): Task[Unit] =
    ZIO.succeed(s"Sent message").delay(1.second).unit

object ActionService:
  val layer = ZLayer.fromFunction(ActionService.apply _)

object Main extends ZIOAppDefault:
  private val handler: Handler[ActionService] =
    ActionMacro.run[ActionService]

  val run =
//    ZIO.debug(handler.call)
    ChatService
      .prompt[ActionService](
        "Slack me a poem in the style of ee cummings about the weather. Also, what's 2156 + 2366."
      )
      .provide(ChatService.live[ActionService], ActionService.layer)
