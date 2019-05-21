package com.adrianrafo.seed.client
package process

import com.adrianrafo.seed.client.common.models.PeopleError
import com.adrianrafo.seed.server.protocol._

trait PeopleServiceClient[F[_]] {

  def getPerson(name: String): F[Either[PeopleError, Person]]

}
