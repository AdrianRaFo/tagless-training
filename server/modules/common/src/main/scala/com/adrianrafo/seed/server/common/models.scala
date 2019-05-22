package com.adrianrafo.seed.server
package common

object models {

  case class SeedServerConfig(name: String, httpPort: Int, rpcPort : Int)

}
