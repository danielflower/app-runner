lazy val root = (project in file(".")).
  settings(
    name := "hello",
    version := "1.0",
    scalaVersion := "2.11.8",
    libraryDependencies += "com.typesafe.akka" %% "akka-http-experimental" % "2.4.8",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    mainClass in assembly := Some("example.akka.WebServer")
  )
