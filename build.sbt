name := "ServiceLevelAgreement"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.2.0-RC3",
  "com.typesafe" % "config" % "1.4.0",
  "org.http4s" %% "http4s-core" % "1.0.0-M4",
  "com.github.blemale" %% "scaffeine" % "3.1.0" % "compile",
  "org.http4s" %% "http4s-dsl" % "1.0.0-M4",
  "org.http4s" %% "http4s-blaze-server" % "1.0.0-M4",
  "org.scalatest" %% "scalatest" % "3.2.0" % "test"
)

scalacOptions ++= Seq(
  "-language:higherKinds",
  "-Xfatal-warnings"
)

mainClass in (Compile, run) := Some("com.Main")