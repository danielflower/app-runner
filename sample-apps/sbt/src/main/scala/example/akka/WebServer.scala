package example.akka

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

object WebServer {

  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()

    val settings: Map[String, String] = sys.env

    // When run from app-runner, you must use the port set in the environment variable APP_PORT
    val port = Integer.parseInt(settings.getOrElse("APP_PORT", "8081"))
    // All URLs must be prefixed with the app name, which is got via the APP_NAME env var.
    val appName = settings.getOrElse("APP_NAME", "my-app")
    val env = settings.getOrElse("APP_ENV", "local") // "prod" or "local"
    println(s"Starting $appName in $env on port $port")

    val route =
      pathPrefix(appName) {
        pathEndOrSingleSlash {
          get {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", port)

    println(s"Server online at http://localhost:$port/$appName")
  }
}
