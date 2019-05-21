package com.adrianrafo.seed.server.app

import java.net.InetAddress

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.adrianrafo.seed.server.common.models._
import com.adrianrafo.seed.server.process.PeopleServiceHandler
import com.adrianrafo.seed.server.protocol._
import io.chrisdavenport.log4cats.Logger
import io.circe.Encoder
import io.circe.syntax._
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.circe._
import shapeless.Poly1

class ServerProgramHTTP[F[_]: ConcurrentEffect: Timer] extends ServerBoot[F] with Http4sDsl[F] {

  implicit val personEncoder: Encoder[Person] = deriveEncoder[Person]

  object PeopleResponseHandler extends Poly1 {

    implicit val peh1 = at[NotFoundError](e => NotFound(e.message.asJson))
    implicit val peh2 = at[DuplicatedPersonError](e => BadRequest(e.message.asJson))
    implicit val peh3 = at[Person](p => Ok(p.asJson))
  }

  def serverProgram(config: SeedServerConfig)(implicit L: Logger[F]): F[ExitCode] = {

    val serverName                    = s"${config.name}"
    implicit val PS: PeopleService[F] = new PeopleServiceHandler[F]

    val httpApp: HttpApp[F] = HttpRoutes
      .of[F] {
        case GET -> Root / "person" / name =>
          PS.getPerson(PeopleRequest(name)).flatMap(_.result.map(PeopleResponseHandler).unify)
      }
      .orNotFound

    for {
      ip <- Effect[F].delay(InetAddress.getByName(host).getHostAddress)
      _  <- L.info(s"$serverName - Starting server at $ip:${config.port}")
      _  <- BlazeServerBuilder[F].bindLocal(config.port).withHttpApp(httpApp).serve.compile.drain
    } yield ExitCode.Success

  }
}

object ServerAppHTTP extends IOApp {
  def run(args: List[String]): IO[ExitCode] = new ServerProgramHTTP[IO].runProgram(args)
}
