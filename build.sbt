ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

val Http4sVersion = "0.23.18"
val JwtHttp4sVersion = "1.2.0"
val JwtScalaVersion = "9.3.0"

val http4sDsl =       "org.http4s"              %% "http4s-dsl"          % Http4sVersion
val emberServer =     "org.http4s"              %% "http4s-ember-server" % Http4sVersion
val jwtHttp4s =       "dev.profunktor"          %% "http4s-jwt-auth"     % JwtHttp4sVersion
val jwtScala =        "com.github.jwt-scala"    %% "jwt-core"            % JwtScalaVersion
val jwtCirce =        "com.github.jwt-scala"    %% "jwt-circe"           % JwtScalaVersion
val munit =           "com.alejandrohdezma"     %% "http4s-munit"        % "0.15.1" % Test


lazy val root = (project in file("."))
  .settings(
    name := "authentication",
  )

libraryDependencies ++= Seq(
  emberServer,
  http4sDsl,
  jwtHttp4s,
  jwtScala,
  jwtCirce,
  munit
)
