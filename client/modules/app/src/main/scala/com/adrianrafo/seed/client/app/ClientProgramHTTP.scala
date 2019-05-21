package com.adrianrafo.seed.client.app

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.adrianrafo.seed.client.common.models._
import io.chrisdavenport.log4cats.Logger
import org.http4s.Uri

import scala.concurrent.ExecutionContext.Implicits.global

class ClientProgramHTTP[F[_]: ConcurrentEffect: ContextShift] extends ClientBoot[F] {

  def clientProgram(config: SeedClientConfig)(implicit L: Logger[F]): F[ExitCode] = {
    for {
      baseUrl <- Effect[F].fromEither(
        Uri.requestTarget(s"http://${config.params.host}:${config.client.port}"))
      (peopleClient, close) <- processServiceHTTP(baseUrl).allocated
      result                <- peopleClient.getPersonProcess(config.params.request)
      _                     <- close
    } yield result.fold(_ => ExitCode.Error, _ => ExitCode.Success)
  }
}

object ClientAppHTTP extends IOApp {
  def run(args: List[String]): IO[ExitCode] = new ClientProgramHTTP[IO].runProgram(args)
}
