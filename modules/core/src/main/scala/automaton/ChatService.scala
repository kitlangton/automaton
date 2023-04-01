package automaton

import zio.*
import zio.http.ZClient
import zio.http.model.Status
import zio.json.*
import zio.openai.Chat
import zio.openai.model.{ChatCompletionRequestMessage, OpenAIFailure, Role}
import zio.schema.Schema

// TODO:
// - allow for multiple actions at once
//   - build sequential actions: construct RPC language with intermediate vars?
//   - the more we allow GPT to compose actions, the fewer messages we need to send (cheaper, faster, stronger)
// - allow for asking for further user input or selections (multiple choice, etc)
final case class ChatService[Service](
    chat: Chat,
    messagesRef: Ref[NonEmptyChunk[ChatCompletionRequestMessage]],
    service: Service,
    actionHandler: Handler[Service]
):
  def prompt(message: String): Task[Unit] =
    for
      _ <- Log.user(message)
      _ <- messagesRef.update(_.appended(ChatCompletionRequestMessage(Role.User, message)))
      _ <- loop()
    yield ()

  def loop(maxDepth: Int = 10): Task[Unit] =
    for
      _              <- ZIO.fail(new Exception("Max depth reached")).when(maxDepth <= 0)
      messages       <- messagesRef.get
      response       <-
        chat.createChatCompletion("gpt-4", messages)
          .catchSome {
            case OpenAIFailure.ErrorResponse(_, _, code, _) if code == Status.TooManyRequests =>
              chat.createChatCompletion("gpt-3.5-turbo", messages)
                .tapError(e =>
                  Log.systemLine("The smart AI is taking too long, so we're using the old AI instead. Results may be less accurate.")
                )
                .mapError(e => new Error(e.toString))
          }
          .mapError(e => new Exception(e.toString))
      response       <- ZIO.from(response.choices.headOption.flatMap(_.message.toOption)).orElseFail(new Error("No response"))
      responseMessage = ChatCompletionRequestMessage(response.role, response.content)

      action <- ZIO
                  .fromEither(response.content.fromJson[RpcCall])
                  .orElseEither(ZIO.succeed(response.content))
      _ <-
        action match
          case Left(rpcCall) =>
            for
              _        <- Log.systemLine(s"${rpcCall.rpc}(${rpcCall.args.mkString(", ")})")
              response <- actionHandler.call(service, rpcCall)
              _        <- Log.system(s" \u2192 ${pprint(response)}")
              _ <- messagesRef.update(
                     _ ++ NonEmptyChunk(responseMessage, ChatCompletionRequestMessage(Role.System, response.toString))
                   ) *> loop(maxDepth - 1)
            yield ()

          case Right(response) =>
            Log.assistant(response)
    yield ()

object ChatService:
  def prompt[Service: Tag](message: String): ZIO[ChatService[Service], Throwable, Unit] =
    ZIO.serviceWithZIO(_.prompt(message))

  inline def live[Service: Tag]: ZLayer[Service, Throwable, ChatService[Service]] =
    val handler = ActionMacro.run[Service]
    val instructions =
      s"""
You can only answer in RPC calls. One at a time. Your final message must be ONLY markdown.

You have access to the following RPC calls:
${handler.description}
To send an RPC call, reply with the following the JSON: { "rpc": "<String>", "args": [<Any>] }
You will receive a response for each RPC call after you send it.
You can only send one RPC call per message.
Only use actions relevant to the request (you don't have to use any actions if they're not helpful).
Once you are done sending RPC calls, finish with a final markdown message to the user.
Your final message should be markdown formatted. This will end the conversation.
To summarize: send one RPC call per message, gathering information to fulfill the request, then send a final markdown message to the user.
DO NOT SEND ANYTHING ELSE. ONLY USE MARKDOWN FOR YOUR FINAL MESSAGE.
Your final message cannot contain any RPC calls. So if you want to send an RPC call, send it before your final message.

example (these are just examples, the actual RPC you have access to are listed above):
{ "rpc": "getWeather", "args": ["New York"] }
<receive response>
{ "rpc": "sendEmail", "args": ["myemail@gmail.com", "Hello!"] }
<receive response>
[Your final message in markdown format]
""".trim

    ZClient.default >>> Chat.live >+>
    ZLayer(Ref.make(NonEmptyChunk(ChatCompletionRequestMessage(Role.System, instructions)))) >>>
      ZLayer.fromFunction(ChatService.apply[Service](_, _, _, handler))

  private[automaton] inline def test[Service: Tag]: ZLayer[Service & zio.openai.Chat, Throwable, ChatService[Service]] =
    val handler = ActionMacro.run[Service]
    val instructions = "Work for me!"

    ZClient.default >+>
    ZLayer(Ref.make(NonEmptyChunk(ChatCompletionRequestMessage(Role.System, instructions)))) >>>
      ZLayer.fromFunction(ChatService.apply[Service](_, _, _, handler))