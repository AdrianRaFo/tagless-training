package com.adrianrafo.seed.server
package app

import java.net.InetAddress

import cats.{Apply, NonEmptyParallel}
import cats.effect._
import cats.implicits._
import com.adrianrafo.seed.server.common.models._
import com.adrianrafo.seed.server.process.PeopleServiceHandler
import com.adrianrafo.seed.server.protocol._
import higherkindness.mu.rpc.server._
import io.chrisdavenport.log4cats.Logger
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto.deriveEncoder
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeServerBuilder
import shapeless.Poly1

class ServerProgram[F[_]: ConcurrentEffect: ContextShift: Timer, M[_]: Apply](implicit p: NonEmptyParallel[F, M]) extends ServerBoot[F] with Http4sDsl[F] {

  implicit val personEncoder: Encoder[Person] = deriveEncoder[Person]

  object PeopleResponseHandler extends Poly1 {

    implicit val peh1 = at[NotFoundError](e => NotFound(e.message.asJson))
    implicit val peh2 = at[DuplicatedPersonError](e => BadRequest(e.message.asJson))
    implicit val peh3 = at[Person](p => Ok(p.asJson))
  }

  def serverProgram(config: SeedServerConfig)(implicit L: Logger[F]): F[ExitCode] = {

    val serverName = s"${config.name}"

    val httpApp: HttpRoutes[F] = {
      implicit val PSHTTP: PeopleService[F] = new PeopleServiceHandler[F]("HTTP")

      HttpRoutes.of[F] {
        case GET -> Root / "person" / name =>
          PSHTTP.getPerson(PeopleRequest(name)).flatMap(_.result.map(PeopleResponseHandler).unify)
      }
    }

    implicit val PSRPC: PeopleService[F] = new PeopleServiceHandler[F]("RPC")

    def joinServices(server: GrpcServer[F]): F[Unit] =
      (
        GrpcServer.server(server),
        BlazeServerBuilder[F]
          .bindLocal(config.httpPort)
          .withHttpApp(httpApp.orNotFound)
          .serve
          .compile
          .drain).parMapN(_ |+| _)

    for {
      peopleService <- PeopleService.bindService[F]
      server        <- GrpcServer.default[F](config.rpcPort, List(AddService(peopleService)))
      ip            <- Effect[F].delay(InetAddress.getByName(host).getHostAddress)
      _             <- L.info(s"$serverName - Starting RPC server at $ip:${config.rpcPort}")
      exitCode      <- joinServices(server).as(ExitCode.Success)
    } yield exitCode

  }
}

object ServerApp extends IOApp {

  def run(args: List[String]): IO[ExitCode] = new ServerProgram[IO, IO.Par].runProgram(args)

}
