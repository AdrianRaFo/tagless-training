package com.adrianrafo.seed.client.app

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.adrianrafo.seed.client.common.models._
import io.chrisdavenport.log4cats.Logger

class ClientProgramRPC[F[_]: ConcurrentEffect: ContextShift] extends ClientBoot[F] {

  def clientProgram(config: SeedClientConfig)(implicit L: Logger[F]): F[ExitCode] = {
    for {
      (peopleClient, close) <- peopleServiceClientRPC(config.params.host, config.client.port).allocated
      result                <- peopleClient.getPerson(config.params.request)
      _                     <- close
    } yield result.fold(_ => ExitCode.Error, _ => ExitCode.Success)
  }
}

object ClientAppRPC extends IOApp {
  def run(args: List[String]): IO[ExitCode] = new ClientProgramRPC[IO].runProgram(args)
}
