package com.adrianrafo.seed.client
package common

object models {

  case class ClientConfig(name: String, httpPort: Int, rpcPort : Int)

  case class ParamsConfig(request: String, host: String)

  case class SeedClientConfig(client: ClientConfig, params: ParamsConfig)

  case class PeopleError(result: String)

}
