package com.adrianrafo.seed.server
package app

import java.net.InetAddress

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.adrianrafo.seed.server.common.models._
import com.adrianrafo.seed.server.process.PeopleServiceHandler
import com.adrianrafo.seed.server.protocol._
import higherkindness.mu.rpc.server._
import io.chrisdavenport.log4cats.Logger

class ServerProgramRPC[F[_]: ConcurrentEffect] extends ServerBoot[F] {

  def serverProgram(config: SeedServerConfig)(implicit L: Logger[F]): F[ExitCode] = {

    val serverName = s"${config.name}"

    implicit val PS: PeopleService[F] = new PeopleServiceHandler[F]

    for {
      peopleService <- PeopleService.bindService[F]
      server        <- GrpcServer.default[F](config.port, List(AddService(peopleService)))
      ip            <- Effect[F].delay(InetAddress.getByName(host).getHostAddress)
      _             <- L.info(s"$serverName - Starting server at $ip:${config.port}")
      exitCode      <- GrpcServer.server(server).as(ExitCode.Success)
    } yield exitCode

  }
}

object ServerAppRPC extends IOApp {
  def run(args: List[String]): IO[ExitCode] = new ServerProgramRPC[IO].runProgram(args)
}
