package automaton.examples.example3

import automaton.*
import zio.*
import java.time.*

case class ActionService(
    appointments: AppointmentService,
    weatherService: WeatherService,
    emailService: EmailService
):
  def existingAppointments: Task[List[Appointment]] =
    appointments.getAppointments

  def weatherOnDate(date: String): UIO[Weather] =
    weatherService.getWeather(date)

  def myEmails: UIO[List[Email]] =
    emailService.emails

  def sendEmail(to: String, body: String): UIO[Unit] =
    emailService.send(to, body)

object ActionService:
  val layer = ZLayer.fromFunction(ActionService.apply _)

object Main extends ZIOAppDefault:
  val run =
    ChatService
      .prompt[ActionService](
        """
          |Can you read my emails and figure out some good times for me to meet people?
          |Let them know for me on my behalf plz.
          |""".stripMargin.trim
      )
      .provide(
        ChatService.live[ActionService],
        ActionService.layer,
        WeatherService.layer,
        AppointmentService.layer,
        EmailService.layer
      )
