package au.com.dius.pact.model

import au.com.dius.pact.consumer._
import au.com.dius.pact.core.model.{Consumer, Provider, RequestResponseInteraction, RequestResponsePact}

/**
  * @deprecated Moved to Kotlin implementation: Use Pact interface instead
  */
@Deprecated
case class PactFragment(consumer: Consumer,
                        provider: Provider,
                        interactions: Seq[RequestResponseInteraction]) {
  import scala.collection.JavaConversions._
  @Deprecated
  def toPact = new RequestResponsePact(provider, consumer, interactions)

  @Deprecated
  def duringConsumerSpec[T](config: MockProviderConfig)(test: => T, verification: ConsumerTestVerification[T]): VerificationResult = {
    val server = DefaultMockProvider(config)
    new ConsumerPactRunner(server).runAndWritePact(toPact, config.getPactVersion)(test, verification)
  }

  //TODO: it would be a good idea to ensure that all interactions in the fragment have the same state
  //      really? why?
  @Deprecated
  def defaultState: Option[String] = interactions.headOption.map(_.getProviderState)

  @Deprecated
  def runConsumer(config: MockProviderConfig, test: TestRun): VerificationResult = {
    duringConsumerSpec(config)(test.run(config), (u:Unit) => None)
  }

  @Deprecated
  def description = s"Consumer '${consumer.getName}' has a pact with Provider '${provider.getName}': " +
    interactions.map { i => i.getDescription }.mkString(" and ") + sys.props("line.separator")

}

/**
  * @deprecated Moved to Kotlin implementation
  */
@Deprecated
object PactFragment {
  @Deprecated
  def consumer(consumer: String) = {
    PactFragmentBuilder.apply(new Consumer(consumer))
  }
}
