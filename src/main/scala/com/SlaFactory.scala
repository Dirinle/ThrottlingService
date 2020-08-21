package com
import java.util.UUID

trait SlaFactory{
  val slas: Map[UUID, Sla]
}
object SlaFactoryImpl extends SlaFactory{
  val sla1 = Sla("user1", 5000)
  val sla2 = Sla("user2", 15)
  val sla3 = Sla("user3", 21)
  val sla4 = Sla("user4", 7)
  val slas: Map[UUID, Sla] = Map(
    UUID.fromString("41aecda5-7814-4882-9543-f3e630e97a56") -> sla1,
    UUID.fromString("1e8da2a1-d309-4a68-8f7a-b0d308907b9f") -> sla1,
    UUID.fromString("afb53e6a-f5b6-4827-9a72-09ec2685648d") -> sla2,
    UUID.fromString("4e0bd1bf-9944-4424-ab6f-ee462857c7da") -> sla3,
    UUID.fromString("9d615a77-983e-49b2-86b9-29e40e53c1ac") -> sla3
  )
}