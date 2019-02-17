package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.Response
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.StateChange
import au.com.dius.pact.provider.StateChangeResult
import au.com.dius.pact.com.github.michaelbull.result.Ok
import spock.lang.Specification

class ProviderVerifierStateChangeSpec extends Specification {

  private ProviderVerifier providerVerifier
  private ProviderInfo providerInfo
  private ConsumerInfo consumer
  private ProviderClient providerClient

  def setup() {
    providerInfo = new ProviderInfo()
    providerVerifier = new ProviderVerifier()
    providerClient = Mock()
  }

  def 'if teardown is set then a statechage teardown request is made after the test'() {
    given:
    def state = new ProviderState('state of the nation')
    def interaction = new RequestResponseInteraction('provider state test', [state],
      new Request(), new Response(200, [:], OptionalBody.body('{}'.bytes)))
    def failures = [:]
    consumer = new ConsumerInfo('Bob', 'http://localhost:2000/hello')
    providerInfo.stateChangeTeardown = true
    GroovyMock(StateChange, global: true)

    when:
    providerVerifier.verifyInteraction(providerInfo, consumer, failures, interaction)

    then:
    1 * StateChange.executeStateChange(*_) >> new StateChangeResult(new Ok([:]), 'interactionMessage')
    1 * StateChange.executeStateChangeTeardown(providerVerifier, interaction, providerInfo, consumer, _)
  }

  def 'if the state change is a closure and teardown is set, executes it with the state change as a parameter'() {
    given:
    def closureArgs = []
    consumer = new ConsumerInfo('Bob', { arg1, arg2 ->
      closureArgs << [arg1, arg2]
      true
    })
    def state = new ProviderState('state of the nation')
    def interaction = new RequestResponseInteraction('provider state test', [state],
      new Request(), new Response(200, [:], OptionalBody.body('{}'.bytes)))
    def failures = [:]
    providerInfo.stateChangeTeardown = true

    when:
    StateChange.executeStateChange(providerVerifier, providerInfo, consumer, interaction, 'state of the nation',
      failures, providerClient)
    StateChange.executeStateChangeTeardown(providerVerifier, interaction, providerInfo, consumer, providerClient)

    then:
    closureArgs == [[state, 'setup'], [state, 'teardown']]
  }

}
