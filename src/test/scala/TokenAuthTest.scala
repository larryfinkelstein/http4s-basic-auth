import TokenAuth.authedRoutes
import cats.effect.IO
import org.http4s.{AuthedRoutes, Status}

class TokenAuthTest extends munit.Http4sAuthedRoutesSuite[String] {

  override val routes: AuthedRoutes[String, IO] = authedRoutes

  test(GET(uri"welcome").context("fred")).alias("Say hello to Fred") { response =>
    assert(response.status == Status.Ok)
    assertIO(response.as[String], "Welcome, fred")
  }

  test(GET(uri"bad").context("authorized")).alias("Bad request") { response =>
    assert(response.status == Status.NotFound)
    assertIO(response.as[String], "Not found")
  }
}
