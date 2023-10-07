import TokenAuth.loginRoutes
import cats.effect.IO
import org.http4s.{HttpRoutes, Status}

class TokenAuthLoginTest extends munit.Http4sHttpRoutesSuite {

  override val routes: HttpRoutes[IO] = loginRoutes

  test(GET(uri"/login")).alias("Login as john") { response =>
    val token = response.cookies.filter(c => c.name == "token").head
    assert(token.name == "token")
    assert(token.content.startsWith("eyJ"))
    println(token.content)
    assert(response.status == Status.Ok)
    assertIO(response.as[String], "Logged In")
  }
}
