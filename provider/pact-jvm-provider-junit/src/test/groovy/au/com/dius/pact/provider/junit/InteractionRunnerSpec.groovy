package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.provider.junit.target.HttpTarget
import au.com.dius.pact.provider.junit.target.Target
import au.com.dius.pact.provider.junit.target.TestTarget
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.TestClass
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

class InteractionRunnerSpec extends Specification {

  @SuppressWarnings('PublicInstanceField')
  class InteractionRunnerTestClass {
    @TestTarget
    public final Target target = new HttpTarget(8332)
  }

  def 'do not publish verification results if any interactions have been filtered'() {
    given:
    def interaction1 = new RequestResponseInteraction(description: 'Interaction 1')
    def interaction2 = new RequestResponseInteraction(description: 'Interaction 2')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1, interaction2 ])

    def clazz = new TestClass(InteractionRunnerTestClass)
    def filteredPact = new FilteredPact(pact, { it.description == 'Interaction 1' })
    def runner = Spy(InteractionRunner, constructorArgs: [clazz, filteredPact, UnknownPactSource.INSTANCE])

    when:
    runner.run([:] as RunNotifier)

    then:
    0 * runner.reportVerificationResults(false)
  }

  @RestoreSystemProperties
  def 'provider version trims -SNAPSHOT'() {
    given:
    System.setProperty('pact.provider.version', '1.0.0-SNAPSHOT-wn23jhd')
    def interaction1 = new RequestResponseInteraction(description: 'Interaction 1')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [ interaction1 ])

    def clazz = new TestClass(InteractionRunnerTestClass)
    def filteredPact = new FilteredPact(pact, { it.description == 'Interaction 1' })
    def runner = new InteractionRunner(clazz, filteredPact, UnknownPactSource.INSTANCE)

    // Property true
    when:
    System.setProperty('pact.provider.version.trimSnapshot', 'true')
    def providerVersion = runner.providerVersion()

    then:
    providerVersion == '1.0.0-wn23jhd'

    // Property false
    when:
    System.setProperty('pact.provider.version.trimSnapshot', 'false')
    providerVersion = runner.providerVersion()

    then:
    providerVersion == '1.0.0-SNAPSHOT-wn23jhd'

    // Property unexpected value
    when:
    System.setProperty('pact.provider.version.trimSnapshot', 'erwf')
    providerVersion = runner.providerVersion()

    then:
    providerVersion == '1.0.0-SNAPSHOT-wn23jhd'

    // Property not present
    when:
    providerVersion = runner.providerVersion()

    then:
    providerVersion == '1.0.0-SNAPSHOT-wn23jhd'
  }

}
