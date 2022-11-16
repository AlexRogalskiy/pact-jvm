package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.support.json.JsonValue
import org.apache.hc.core5.http.ContentType
import spock.lang.Issue
import spock.lang.Specification

import static au.com.dius.pact.consumer.dsl.PactDslResponse.DEFAULT_JSON_CONTENT_TYPE_REGEX

class PactDslResponseSpec extends Specification {

  def 'allow matchers to be set at root level'() {
    expect:
    response.matchingRules.rulesForCategory('body').matchingRules == [
      '$': new MatchingRuleGroup([TypeMatcher.INSTANCE])]

    where:
    pact = ConsumerPactBuilder.consumer('complex-instruction-service')
      .hasPactWith('workflow-service')
      .uponReceiving('a request to start a workflow')
      .path('/startWorkflowProcessInstance')
      .willRespondWith()
      .body(PactDslJsonRootValue.numberType())
      .toPact()
    interaction = pact.interactions.first()
    response = interaction.response
  }

  def 'default json content type should match common variants'() {
    expect:
      acceptableDefaultContentType.matches(DEFAULT_JSON_CONTENT_TYPE_REGEX) == matches

    where:
      acceptableDefaultContentType            | matches
      'application/json;charset=utf-8'        | true
      'application/json; charset=UTF-8'       | true
      'application/json; charset=utf-8'       | true
      'application/json;charset=iso-8859-1'   | true
      'application/json'                      | true
      ContentType.APPLICATION_JSON.toString() | true
      'application/json;foo=bar'              | false
      'application/json;charset=*'            | false
      'application/xml'                       | false
      'foo'                                   | false
  }

  def 'sets up any default state when created'() {
    given:
    ConsumerPactBuilder consumerPactBuilder = ConsumerPactBuilder.consumer('spec')
    PactDslRequestWithPath request = new PactDslRequestWithPath(consumerPactBuilder, 'spec', 'spec', [], 'test', '/',
      'GET', [:], [:], OptionalBody.empty(), new MatchingRulesImpl(), new Generators(), null, null)
    PactDslResponse defaultResponseValues = new PactDslResponse(consumerPactBuilder, request, null, null)
      .headers(['test': 'test'])
      .body('{"test":true}')
      .status(499)

    when:
    PactDslResponse subject = new PactDslResponse(consumerPactBuilder, request, null, defaultResponseValues)

    then:
    subject.responseStatus == 499
    subject.responseHeaders == [test: ['test']]
    subject.responseBody == OptionalBody.body('{"test":true}'.bytes)
  }

  @Issue('#716')
  def 'set the content type header correctly'() {
    given:
    def builder = ConsumerPactBuilder.consumer('spec').hasPactWith('provider')
    def body = new PactDslJsonBody().numberValue('key', 1).close()

    when:
    def pact = builder
      .given('Given the body method is invoked before the header method')
      .uponReceiving('a request for some response')
      .path('/bad/content-type/matcher')
      .method('GET')
      .willRespondWith()
      .status(200)
      .body(body)
      .matchHeader('Content-Type', 'application/json')

      .given('Given the body method is invoked after the header method')
      .uponReceiving('a request for some response')
      .path('/no/content-type/matcher')
      .method('GET')
      .willRespondWith()
      .status(200)
      .matchHeader('Content-Type', 'application/json')
      .body(body)
      .toPact()

    def responses = pact.interactions*.response

    then:
    responses[0].matchingRules.rulesForCategory('header').matchingRules['Content-Type'].rules == [
      new RegexMatcher('application/json')
    ]
    responses[1].matchingRules.rulesForCategory('header').matchingRules['Content-Type'].rules == [
      new RegexMatcher('application/json')
    ]
  }

  @Issue('#748')
  def 'uponReceiving should pass the path on'() {
    given:
    def builder = ConsumerPactBuilder.consumer('spec').hasPactWith('provider')

    when:
    def pact = builder
      .uponReceiving('a request for response No 1')
      .path('/response/1')
      .method('GET')
      .willRespondWith()
      .status(200)
      .uponReceiving('a request for the same path')
      .willRespondWith()
      .status(200)
      .toPact()

    then:
    pact.interactions*.request.path == ['/response/1', '/response/1']
  }

  @Issue('#1121')
  def 'content type header is case sensitive'() {
    given:
    def builder = ConsumerPactBuilder.consumer('spec').hasPactWith('provider')

    when:
    def response = builder.uponReceiving('a request for response No 1')
      .path('/')
      .willRespondWith()
      .headers(['content-type': 'text/plain'])
      .body(new PactDslJsonBody())

    then:
    response.responseHeaders == ['content-type': ['text/plain']]
  }

  def 'allows setting any additional metadata'() {
    given:
    def builder = ConsumerPactBuilder.consumer('complex-instruction-service')
      .hasPactWith('workflow-service')
      .uponReceiving('a request to start a workflow')
      .path('/startWorkflowProcessInstance')
      .willRespondWith()
      .body(PactDslJsonRootValue.numberType())

    when:
    def pact = builder.addMetadataValue('test', 'value').toPact()

    then:
    pact.metadata.findAll {
      !['pactSpecification', 'pact-jvm', 'plugins'].contains(it.key)
    } == [test: new JsonValue.StringValue('value')]
  }

  @Issue('#1611')
  def 'supports empty bodies'() {
    given:
    def builder = ConsumerPactBuilder.consumer('empty-body-consumer')
      .hasPactWith('empty-body-service')
      .uponReceiving('a request for an empty body')
      .path('/path')
      .willRespondWith()
      .body('')

    when:
    def pact = builder.toPact()
    def interaction = pact.interactions.first()
    def pactV4 = builder.toPact(V4Pact)
    def v4Interaction = pactV4.interactions.first()

    then:
    interaction.response.body.state == OptionalBody.State.EMPTY
    interaction.toMap(PactSpecVersion.V3).response == [status: 200, body: '']
    v4Interaction.response.body.state == OptionalBody.State.EMPTY
    v4Interaction.toMap(PactSpecVersion.V4).response == [status: 200, body: [content: '']]
  }

  @Issue('#1623')
  def 'supports setting a content type matcher'() {
    given:
    def response = ConsumerPactBuilder.consumer('spec')
      .hasPactWith('provider')
      .uponReceiving('a XML request')
      .path('/path')
      .willRespondWith()
    def example = '<?xml version=\"1.0\" encoding=\"utf-8\"?><example>foo</example>'

    when:
    def result = response.bodyMatchingContentType('application/xml', example)

    then:
    response.responseHeaders['Content-Type'] == ['application/xml']
    result.responseBody.valueAsString() == example
    result.responseMatchers.rulesForCategory('body').toMap(PactSpecVersion.V4) == [
      '$': [matchers: [[match: 'contentType', value: 'application/xml']], combine: 'AND']
    ]
  }
}
