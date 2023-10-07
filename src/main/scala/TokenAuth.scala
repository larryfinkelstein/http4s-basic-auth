import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._
import com.comcast.ip4s._
import dev.profunktor.auth._
import dev.profunktor.auth.jwt._
import io.circe._
import io.circe.parser._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.ember.server._
import org.http4s.server.Server
import pdi.jwt._

import java.time.Instant

object TokenAuth extends IOApp {
  case class AuthUser(id: Long, name: String)
  case class TokenPayLoad(user: String, level: String)

  object TokenPayLoad {
    implicit def decoder: Decoder[TokenPayLoad] = Decoder.instance { h =>
      for {
        user <- h.get[String]("user")
        level <- h.get[String]("level")
      } yield TokenPayLoad(user, level)
    }
  }

  private val claim = JwtClaim(content = """{"user":"John", "level":"basic"}""",
    expiration = Some(Instant.now.plusSeconds(60*15).getEpochSecond),
    issuedAt = Some(Instant.now.getEpochSecond))

  private val key = "secretKey"

  private val algo = JwtAlgorithm.HS256

  private val token = JwtCirce.encode(claim, key, algo)

  private val database = Map("John" -> "JohnDoe")

  private val authenticate: JwtToken => JwtClaim => IO[Option[String]] = (token: JwtToken) => (claim: JwtClaim) => {
    decode[TokenPayLoad](claim.content) match {
      case Right(payload) => IO(database.get(payload.user))
      case Left(_) => IO(None)
    }
  }

  private val jwtAuth = JwtAuth.hmac(key, algo)
  private val middleware = JwtAuthMiddleware[IO, String](jwtAuth, authenticate)

  val authedRoutes: AuthedRoutes[String, IO] =
    AuthedRoutes.of {
      case GET -> Root / "welcome" as user =>
        Ok(s"Welcome, $user")
    }

  val loginRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "login" =>
        Ok(s"Logged In").map(_.addCookie(ResponseCookie("token", token)))
    }

  private val securedRoutes: HttpRoutes[IO] = middleware(authedRoutes)

  private val service = loginRoutes <+> securedRoutes

  val server: Resource[IO, Server] = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8080")
    .withHttpApp(service.orNotFound)
    .build

  override def run(args: List[String]): IO[ExitCode] = server.use(_ => IO.never).as(ExitCode.Success)
}
