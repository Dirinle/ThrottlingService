package com

import java.util.UUID

import cats.Applicative
import cats.effect.ConcurrentEffect

trait SlaService [F[_]]{
  def getSlaByToken(token: UUID): F[Sla]
}
class SlaServiceCached [F[_]: ConcurrentEffect] (slaFactory: SlaFactory) extends SlaService[F] {
  def getSlaByToken(token: UUID): F[Sla] = {
    Thread.sleep(250)
    Applicative[F].pure(slaFactory.slas.getOrElse(token, Sla.default))
  }
}
