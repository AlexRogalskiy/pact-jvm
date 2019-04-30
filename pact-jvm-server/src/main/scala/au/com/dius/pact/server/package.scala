package au.com.dius.pact

import au.com.dius.pact.core.model.RequestResponseInteraction

package object server {
  type ServerState = Map[String, StatefulMockProvider[RequestResponseInteraction]]
}
