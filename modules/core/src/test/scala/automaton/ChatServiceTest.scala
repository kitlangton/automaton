package automaton

import zio.*
import zio.http.ZClient
import zio.http.model.Status
import zio.openai.Chat
import zio.openai.model.{CreateChatCompletionRequest, CreateChatCompletionResponse, OpenAIFailure}
import zio.test.*

case class SimpleService():
  val printHello =
    ZIO.debug("Hello from your code")
object ChatServiceTest extends ZIOSpecDefault:
  def spec =
    suite("ChatServiceTest")(
      test("falls back when preferred model is overload")(
        for

          _ <- ChatService.prompt[SimpleService]("Greet me")
        yield assertCompletes
      ).provide(
        ChatService.live[SimpleService], ZLayer.succeed(SimpleService()),
        ZLayer.succeed(
          new Chat:
            override def createChatCompletion(body: CreateChatCompletionRequest): ZIO[Any, OpenAIFailure, CreateChatCompletionResponse] =
              ZIO.fail(OpenAIFailure.ErrorResponse(zio.http.URL.empty, zio.http.model.Method.PUT, Status.TooManyRequests, zio.openai.model.Error("forced tooManyRequests", `type` = "someType", None, None))
            )
      )
    )
    )
