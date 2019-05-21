package com.adrianrafo.seed.client.process
package runtime

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.either._
import com.adrianrafo.seed.client.common.models.PeopleError
import com.adrianrafo.seed.server.protocol.Person
import io.chrisdavenport.log4cats.Logger
import io.circe._
import io.circe.generic.semiauto._
import org.http4s.Status.Successful
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

object PeopleServiceClientHTTP {

  val serviceName = "PeopleClientHTTP"

  implicit val personDecoder: Decoder[Person] = deriveDecoder[Person]

  def apply[F[_]: Effect](client: Client[F], baseUrl: Uri)(
      implicit L: Logger[F]): PeopleServiceClient[F] =
    new PeopleServiceClient[F] {

      implicit val personEntity: EntityDecoder[F, Person] = jsonOf[F, Person]

      def getPerson(name: String): F[Either[PeopleError, Person]] =
        for {
          response <- client.fetch {
            Request[F](method = Method.GET, uri = baseUrl / "person" / name)
          }(handleResponse)
          _ <- L.info(s"$serviceName - Request: $name - Result: $response")
        } yield response

      def handleResponse: Response[F] => F[Either[PeopleError, Person]] = {
        case Successful(response) => response.as[Person].map(_.asRight[PeopleError])
        case err                  => err.as[String].map(PeopleError(_).asLeft[Person])
      }
    }

  def createClient[F[_]: ConcurrentEffect: Logger](baseUrl: Uri)(
      implicit EC: ExecutionContext): Resource[F, PeopleServiceClient[F]] =
    BlazeClientBuilder(EC).resource.map(PeopleServiceClientHTTP(_, baseUrl))

}
