package au.com.dius.pact.model

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import java.util.function.Predicate

/**
 * Pact between a consumer and a provider
 */
@CompileStatic
@ToString(includeSuper = true)
@EqualsAndHashCode(callSuper = true)
class RequestResponsePact extends BasePact<RequestResponseInteraction> {
  List<RequestResponseInteraction> interactions

  RequestResponsePact(Provider provider, Consumer consumer, List<RequestResponseInteraction> interactions) {
    this(provider, consumer, interactions, DEFAULT_METADATA)
  }

  RequestResponsePact(Provider provider, Consumer consumer, List<RequestResponseInteraction> interactions,
                      Map metadata) {
    super(provider, consumer, metadata)
    this.interactions = interactions
  }

  Pact<RequestResponseInteraction> sortInteractions() {
    interactions = new ArrayList<RequestResponseInteraction>(interactions).sort { it.providerState + it.description }
    this
  }

  @Override
  @SuppressWarnings('SpaceAroundMapEntryColon')
  Map toMap(PactSpecVersion pactSpecVersion) {
    [
      provider      : objectToMap(provider),
      consumer      : objectToMap(consumer),
      interactions  : interactions*.toMap(pactSpecVersion),
      metadata      : metaData(this.metadata, pactSpecVersion)
    ]
  }

  @Override
  void mergeInteractions(List interactions) {
    this.interactions = (this.interactions + (interactions as List<RequestResponseInteraction>))
      .unique { it.uniqueKey() }
    sortInteractions()
  }

  RequestResponseInteraction interactionFor(String description, String providerState) {
    interactions.find { i ->
      i.description == description && i.providerStates.any { it.name == providerState }
    }
  }

  /**
   * @deprecated Wrap the pact in a FilteredPact instead
   */
  @Override
  @Deprecated
  Pact<RequestResponseInteraction> filterInteractions(Predicate predicate) {
    new FilteredPact(this, predicate)
  }
}
