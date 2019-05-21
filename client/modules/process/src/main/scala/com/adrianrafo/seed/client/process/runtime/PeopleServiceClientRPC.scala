package com.adrianrafo.seed.client.process
package runtime

import java.net.InetAddress

import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect.{ConcurrentEffect, ContextShift, Effect, Resource}
import com.adrianrafo.seed.client.common.models.PeopleError
import com.adrianrafo.seed.client.process.runtime.handlers._
import com.adrianrafo.seed.server.protocol.{PeopleRequest, PeopleService, Person}
import higherkindness.mu.rpc.ChannelForAddress
import higherkindness.mu.rpc.channel.{ManagedChannelInterpreter, UsePlaintext}
import io.chrisdavenport.log4cats.Logger
import io.grpc.{CallOptions, ManagedChannel}

object PeopleServiceClientRPC {

  val serviceName = "PeopleClientRPC"

  def apply[F[_]: Effect](client: PeopleService[F])(implicit L: Logger[F]): PeopleServiceClient[F] =
    new PeopleServiceClient[F] {

      def getPerson(name: String): F[Either[PeopleError, Person]] =
        for {
          response <- client.getPerson(PeopleRequest(name))
          _ <- L.info(
            s"$serviceName - Request: $name - Result: ${response.result.map(PeopleResponseLogger).unify}")
        } yield response.result.map(PeopleResponseHandler).unify

    }

  def createClient[F[_]: ContextShift: Logger](
      hostname: String,
      port: Int,
      sslEnabled: Boolean = true)(
      implicit F: ConcurrentEffect[F]): Resource[F, PeopleServiceClient[F]] = {

    val channel: F[ManagedChannel] =
      F.delay(InetAddress.getByName(hostname).getHostAddress).flatMap { ip =>
        val channelFor    = ChannelForAddress(ip, port)
        val channelConfig = if (!sslEnabled) List(UsePlaintext()) else Nil
        new ManagedChannelInterpreter[F](channelFor, channelConfig).build
      }

    def clientFromChannel: Resource[F, PeopleService[F]] =
      PeopleService.clientFromChannel(channel, CallOptions.DEFAULT)

    clientFromChannel.map(PeopleServiceClientRPC(_))
  }

}
