package com

import cats.Applicative
import cats.effect.ConcurrentEffect
import org.http4s.{HttpRoutes, Response, Status}
import cats.implicits._
import org.http4s.dsl.Http4sDsl

class HttpServer[F[_]: ConcurrentEffect](throttlingService: ThrottlingService[F])
  extends Http4sDsl[F] {
  val server = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(token) => throttlingService.isRequestAllowed(token).flatMap { x =>
        val status: Status = if (x) Ok else Forbidden
        Applicative[F].pure(Response[F](status = status))
      }
    case GET -> Root  => Applicative[F].pure(Response[F](status = Unauthorized))
  }
}
