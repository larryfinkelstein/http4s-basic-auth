import cats.data._
import cats.effect._
import com.comcast.ip4s._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.ember.server._
import org.http4s.headers.Authorization
import org.http4s.implicits._
import org.http4s.server._

object BasicExample extends IOApp {

  val authUserEither: Kleisli[IO, Request[IO], Either[String, String]] = Kleisli { req =>
    val authHeader: Option[Authorization] = req.headers.get[Authorization]
    authHeader match {
      case Some(value) =>
        value match {
          case Authorization(BasicCredentials(creds)) =>
            if (creds == ("username", "password"))
              IO(Right(creds._1))
            else
              IO(Left("Unauthorized"))
          case _ => IO(Left("No basic credentials"))
        }
      case None => IO(Left("Unauthorized"))
    }
  }

  val authedRoutes: AuthedRoutes[String, IO] =
    AuthedRoutes.of {
      case GET -> Root / "welcome" as user =>
        Ok(s"Welcome, $user")
    }

  val onFailure: AuthedRoutes[String, IO] = Kleisli { (req: AuthedRequest[IO, String]) =>
    req.req match {
      case _ => OptionT.pure[IO](Response[IO](status = Status.Unauthorized))
    }
  }

  val authMiddleware: AuthMiddleware[IO, String] = AuthMiddleware(authUserEither, onFailure)

  val serviceKleisli: HttpRoutes[IO] = authMiddleware(authedRoutes)

  val server: Resource[IO, Server] = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8080")
    .withHttpApp(serviceKleisli.orNotFound)
    .build

  override def run(args: List[String]): IO[ExitCode] = server.use(_ => IO.never).as(ExitCode.Success)
}