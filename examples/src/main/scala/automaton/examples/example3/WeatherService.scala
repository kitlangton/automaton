package automaton.examples.example3

import zio.*

final case class Weather(
    fahrenheit: Int = scala.util.Random.nextInt(70) + 20,
    chanceOfRain: Double = scala.util.Random.nextDouble()
):
  override def toString: String =
    s"Weather(fahrenheit=$fahrenheit, chanceOfRain=$chanceOfRain)"

final case class WeatherService():
  def getWeather(date: String): UIO[Weather] = ZIO.succeed(Weather())

object WeatherService:
  val layer = ZLayer.fromFunction(WeatherService.apply _)
