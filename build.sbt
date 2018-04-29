import play.sbt.PlayScala

name := "SyndicationWrite"

version := "0.1"

scalaVersion := "2.12.4"

val akka = "2.5.8"
val json4sVersion = "3.5.3"
val mongo = "0.12.7"
val http = "10.0.5"
val mail = "1.4"
val scalaTest = "3.0.1"
val akkaTest = "2.5.4"
val playVersion = "2.6.11"
val mongoDriver = "3.4.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akka,
  "com.typesafe.akka" %% "akka-http" % http,
  "com.typesafe.akka" %% "akka-http-core" % http,
  "org.json4s" %% "json4s-jackson" % json4sVersion,
  "org.reactivemongo" %% "reactivemongo" % mongo,
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "org.apache.commons" % "commons-email" % mail,
  "com.typesafe.play" %% "play-ws" % playVersion,
  "org.mongodb" % "mongo-java-driver" % mongoDriver

)

libraryDependencies += jdbc
libraryDependencies += ws
libraryDependencies += filters
libraryDependencies += guice