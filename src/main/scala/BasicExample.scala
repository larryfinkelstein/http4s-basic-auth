import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server._
import org.http4s.implicits._
import org.http4s.ember.server._
import com.comcast.ip4s._

import cats.data._
import org.http4s.Credentials
import org.http4s.headers.Authorization

object BasicExample extends IOApp {

  case class User(id: Long, name: String)

  val authUserEither: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli { req =>
    val authHeader: Option[Authorization] = req.headers.get[Authorization]
    authHeader match {
      case Some(value) =>
        value match {
          case Authorization(BasicCredentials(creds)) =>
            if (creds == ("username", "password"))
              IO(Right(User(1, creds._1)))
            else
              IO(Left("Unauthorized"))
          case _ => IO(Left("No basic credentials"))
        }
      case None => IO(Left("Unauthorized"))
    }
  }

//  val userMiddleware: AuthMiddleware[IO, User] = AuthMiddleware(authUserEither)

  val authedRoutes: AuthedRoutes[User, IO] =
    AuthedRoutes.of {
      case GET -> Root / "welcome" as user =>
        Ok(s"Welcome, ${user.name}")
    }

  val onFailure: AuthedRoutes[String, IO] = Kleisli { (req: AuthedRequest[IO, String]) =>
    req.req match {
      case _ => OptionT.pure[IO](Response[IO](status = Status.Unauthorized))
    }
  }

  val authMiddleware: AuthMiddleware[IO, User] = AuthMiddleware(authUserEither, onFailure)

  val serviceKleisli: HttpRoutes[IO] = authMiddleware(authedRoutes)

  val server: Resource[IO, Server] = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8080")
    .withHttpApp(serviceKleisli.orNotFound)
    .build

  override def run(args: List[String]): IO[ExitCode] = server.use(_ => IO.never).as(ExitCode.Success)
}