package com.adrianrafo.seed.client.process

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.either._
import com.adrianrafo.seed.client.common.models
import com.adrianrafo.seed.client.common.models.PeopleError
import com.adrianrafo.seed.server.protocol.Person

class ProcessService[F[_]: Sync](PSC: PeopleServiceClient[F]) {

  def getPersonProcess(name: String): F[Either[models.PeopleError, Person]] =
    if (name.nonEmpty) PSC.getPerson(name)
    else PeopleError("Person name cannot be empty").asLeft[Person].pure[F]

}
