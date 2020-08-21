package com

case class Sla(user:String, rps:Int)

object Sla{
  val default = Sla("default", 100)
}