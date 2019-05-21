package com.adrianrafo.seed.client
package app

import cats.effect._
import cats.syntax.functor._
import cats.syntax.flatMap._
import com.adrianrafo.seed.client.common.models._
import com.adrianrafo.seed.client.process.ProcessService
import com.adrianrafo.seed.client.process.runtime.{PeopleServiceClientHTTP, PeopleServiceClientRPC}
import com.adrianrafo.seed.config.ConfigService
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.Uri
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext

abstract class ClientBoot[F[_]: ConcurrentEffect: ContextShift] {

  def processServiceHTTP(baseUrl: Uri)(
      implicit L: Logger[F],
      EC: ExecutionContext): Resource[F, ProcessService[F]] =
    PeopleServiceClientHTTP.createClient(baseUrl).map(new ProcessService[F](_))

  def processServiceRPC(host: String, port: Int)(
      implicit L: Logger[F]): Resource[F, ProcessService[F]] =
    PeopleServiceClientRPC.createClient(host, port, sslEnabled = false).map(new ProcessService[F](_))

  def runProgram(args: List[String]): F[ExitCode] = {
    def setupConfig: F[SeedClientConfig] =
      ConfigService[F]
        .serviceConfig[ClientConfig]
        .map(client => SeedClientConfig(client, ClientParams.loadParams(client.name, args)))

    for {
      config   <- setupConfig
      logger   <- Slf4jLogger.fromName[F](config.client.name)
      exitCode <- clientProgram(config)(logger)
    } yield exitCode
  }

  def clientProgram(config: SeedClientConfig)(implicit L: Logger[F]): F[ExitCode]
}
