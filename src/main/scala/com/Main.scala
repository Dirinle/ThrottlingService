package com

import cats.effect.{ContextShift, IO, Timer}
import cats.effect.IO._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._


object Main {
  val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val cs = ContextShift(ContextShift[IO](IO.contextShift(ec)))
  implicit val timer = IO.timer(ec)
  val configService = ConfigServiceImpl
  val slaService = new SlaServiceCached[IO](SlaFactoryImpl)
  val throttlingService = new ThrottlingServiceImp[IO](slaService, configService)
  val server = new HttpServer[IO](throttlingService)
  def main(args: Array[String]): Unit = {
    val serverBuilder = BlazeServerBuilder[IO](ec)
      .bindHttp(8080, "0.0.0.0").withHttpApp(Router("/" -> server.server).orNotFound)
    serverBuilder.resource
      .use(_ => IO.never)
      .unsafeRunSync()

  }
}
