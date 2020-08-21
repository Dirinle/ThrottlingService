package com

import java.util.UUID

import cats.effect.{ConcurrentEffect, Sync}
import cats.{Applicative, Functor}
import cats.implicits._
import com.github.benmanes.caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.blemale.scaffeine.{Cache, Scaffeine}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait ThrottlingService [F[_]] {
  val graceRps:Int // configurable
  val slaService: SlaService[F] // use mocks/stubs for testing
  // Should return true if the request is within allowed RPS.
  val isRequestAllowed: UUID => F[Boolean]
}

class ThrottlingServiceImp[F[_]: ConcurrentEffect](val slaService: SlaService[F], configService: ConfigService)
  extends ThrottlingService[F]{
  override val graceRps: Int = configService.graceRps
  private def cache [T, Y] (ttl: FiniteDuration, size: Long)(onRemove: (T, Y, caffeine.cache.RemovalCause) => Unit = (_:T, _:Y, _:caffeine.cache.RemovalCause) => ()) =
    Scaffeine().recordStats().expireAfterWrite(ttl).maximumSize(size).removalListener(onRemove).build[T, Y]()

  private val slaCache = cache[UUID, Sla](configService.slaCacheTtlSeconds.seconds, configService.slaCacheSize)()
  private val usersRequestsLeft = cache[Sla, Int](850.milliseconds, configService.usersCacheSize)()
  private def moveToSecondCache(sla: Sla, firstRequestsLeftAndAdditional: (Int, Boolean), c: caffeine.cache.RemovalCause) = if (c == RemovalCause.EXPIRED) {
    println("EXPIRED")
      val requestsLeft =
        if (firstRequestsLeftAndAdditional._1 <= 0) (sla.rps*0.1).toInt
        else firstRequestsLeftAndAdditional._1
      usersRequestsLeft.put(sla, requestsLeft)
    }

  private val usersFirstRequestsLeft = cache[Sla, (Int, Boolean)](150.milliseconds, configService.usersCacheSize)(moveToSecondCache)

  private def getSla(token: UUID): F[Sla] = {
    val maybeRps: Option[Sla] = slaCache.getIfPresent(token)
    maybeRps match {
      case Some(sla) => Applicative[F].pure(sla)
      case None =>
        slaService.getSlaByToken(token).map{sla =>
          slaCache.put(token, sla)
          sla
        }

    }
  }

  private def updateRequests(requests:Cache[Sla, Int], sla: Sla, firstCache: Boolean): Option[Boolean] =
    requests.getIfPresent(sla).map{ requestsLeft =>
      if (requestsLeft > 0){
        requests.put(sla, requestsLeft - 1)
        true
      }
      else false
  }

  val isRequestAllowed: UUID => F[Boolean] = t =>
      getSla(t).map{ sla =>
        usersFirstRequestsLeft.getIfPresent(sla).map{ case (requestsLeft, isAdditional) =>
          if (requestsLeft > 0){
            usersFirstRequestsLeft.put(sla, (requestsLeft - 1, isAdditional))
            true
          }
          else if (!isAdditional){
            val newRequests = (sla.rps*0.1).toInt -1
            usersFirstRequestsLeft.put(sla, (newRequests, true))
            true
          }
          else false
        }.orElse{
          usersRequestsLeft.getIfPresent(sla).map{ requestsLeft =>
            if (requestsLeft > 0){
              usersRequestsLeft.put(sla, requestsLeft - 1)
              true
            }
            else false
          }
        }.getOrElse{
          usersFirstRequestsLeft.put(sla, (sla.rps -1, false))
          true
        }
      }
}