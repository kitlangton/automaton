package automaton.examples.example3

import zio.*

import java.time.LocalDateTime

final case class Appointment(dateTime: LocalDateTime, minutes: Int):
  override def toString: String = s"Appointment(dateTime=$dateTime, minutes=$minutes)"

final case class AppointmentService():
  def getAppointments: Task[List[Appointment]] =
    ZIO.succeed(
      List(
        Appointment(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0), 120),
        Appointment(LocalDateTime.now().plusDays(2).withHour(11).withMinute(0), 30),
        Appointment(LocalDateTime.now().plusDays(3).withHour(12).withMinute(0), 30)
      )
    )

object AppointmentService:
  val layer = ZLayer.fromFunction(AppointmentService.apply _)
