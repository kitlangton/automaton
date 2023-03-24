package automaton.examples.example3

import automaton.Log
import zio.*

final case class Email(from: String, title: String, body: String):
  override def toString: String =
    s"Email(from=$from, title=$title, body=$body)"

object Email:
  val examples =
    List(
      Email(
        "Mom",
        "Lunch Meeting",
        "Can we plan a lunch for tomorrow or the day after? I have a few things I'd like to discuss with you."
      ),
      Email(
        "Biff",
        "Re: Football",
        "When are we going to play Football? I'm ready to kick some butt! Sometime in the next three days would be great. Make sure it's not raining!!!"
      ),
      Email(
        "Spam",
        "I wAnT YoUr MoNeY",
        "giMmE All uR Money Puleze! Can we hang out?"
      )
    )

final case class EmailService():
  def emails: UIO[List[Email]] = ZIO.succeed(Email.examples)

  def send(to: String, body: String) =
    Log.system(s"Sending email to '$to' with body '$body'")

object EmailService:
  val layer = ZLayer.fromFunction(EmailService.apply _)
