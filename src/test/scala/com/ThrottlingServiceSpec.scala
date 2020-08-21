package com

import java.util.UUID

//import cats.effect.IO._
import cats.effect.{ContextShift, IO}
import org.scalatest.flatspec._
import org.scalatest.matchers._

class ThrottlingServiceSpec extends AnyFlatSpec with should.Matchers {
  val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val cs = ContextShift(ContextShift[IO](IO.contextShift(ec)))
  implicit val timer = IO.timer(ec)
  val configService = new ConfigService{
    val graceRps: Int = 10
    val slaCacheTtlSeconds: Int = 100
    val slaCacheSize: Int = 100
    val usersCacheSize: Int = 100
  }
  val uuid1 = UUID.randomUUID()
  val uuid2 = UUID.randomUUID()
  val sla1 = Sla("user", 20)
  val sla2 = Sla("user", 15)
  val slaFactory = new SlaFactory {
    val slas: Map[UUID, Sla] = Map(uuid1 -> sla1, uuid2 -> sla2)
  }
  val slaService = new SlaServiceCached[IO](slaFactory)
  val throttlingService = new ThrottlingServiceImp[IO](slaService, configService)
  def countSuccess(requests: Int, token: UUID): Int = (0 to requests).map(_ => throttlingService.isRequestAllowed(token).unsafeRunSync()).count(_==true)

  "ThrottlingService" should "give 10% more requests if at first 0.1 sec all requests is used" in {
    countSuccess(sla1.rps * 2, uuid1) shouldBe (sla1.rps*1.1).toInt
  }
  "ThrottlingService" should "use max rps in normal usage" in {
    val first = countSuccess((sla2.rps*0.5).toInt, uuid2)
    Thread.sleep(100)
    val second = countSuccess(sla2.rps*2, uuid2)
    first + second shouldBe sla2.rps

  }
}
