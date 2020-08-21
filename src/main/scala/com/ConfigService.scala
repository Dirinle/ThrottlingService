package com

import com.typesafe.config.ConfigFactory

trait ConfigService {
  val graceRps: Int
  val slaCacheTtlSeconds: Int
  val slaCacheSize: Int
  val usersCacheSize: Int
}
object ConfigServiceImpl extends ConfigService{
  private val config = ConfigFactory.load()
  val graceRps = config.getInt("graceRps")
  val slaCacheTtlSeconds = config.getInt("slaCacheTtlSeconds")
  val slaCacheSize = config.getInt("slaCacheSize")
  val usersCacheSize: Int = config.getInt("usersCacheSize")
}
