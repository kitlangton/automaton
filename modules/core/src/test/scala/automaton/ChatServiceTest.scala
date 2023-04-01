package automaton

import zio.*
import zio.http.ZClient
import zio.http.model.Status
import zio.openai.Chat
import zio.openai.model.{CreateChatCompletionRequest, CreateChatCompletionResponse, OpenAIFailure}
import zio.openai.model.ChatCompletionResponseMessage
import zio.prelude.data.Optional
import zio.test.*

case class SimpleService():
  val printHello =
    ZIO.debug("Hello from your code")
object ChatServiceTest extends ZIOSpecDefault:
  def spec =
    suite("ChatServiceTest")(
      test("falls back when preferred model is overload")(
        for
          calledFallbackModel <- Promise.make[Nothing, Unit]
          _ <- ChatService.prompt[SimpleService]("Fail for me.")
            .provide(
              ChatService.test[SimpleService], ZLayer.succeed(SimpleService()),
              ZLayer.succeed(
                new Chat:
                  override def createChatCompletion(body: CreateChatCompletionRequest): ZIO[Any, OpenAIFailure, CreateChatCompletionResponse] =
                    body.model match
                      case "gpt-4" => ZIO.fail(OpenAIFailure.ErrorResponse(zio.http.URL.empty, zio.http.model.Method.PUT, Status.TooManyRequests, zio.openai.model.Error("forced tooManyRequests", `type` = "someType", None, None)))
                      case "gpt-3.5-turbo" =>
                        calledFallbackModel.succeed(()) *>
                        ZIO.succeed(
                        CreateChatCompletionResponse(
                          "id",
                          "object",
                          created = 0,
                          model = "dumb",
                          choices = Chunk(
                            CreateChatCompletionResponse.ChoicesItem(
                              index = Optional.Present(1),
                              message = Optional.Present(
                                ChatCompletionResponseMessage(
                                  zio.openai.model.Role.Assistant,
                                  content = "Response from slow assistance")
                              ),
                              finishReason = Optional.Absent
                            )
                          )
                        )
                      )
              )
            )
          expectationMet <- calledFallbackModel.isDone
        yield assertTrue(expectationMet)
      )
    )
