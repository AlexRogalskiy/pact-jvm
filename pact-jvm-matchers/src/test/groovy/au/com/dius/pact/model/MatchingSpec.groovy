package au.com.dius.pact.model

import au.com.dius.pact.matchers.BodyMismatch
import scala.None$
import scala.Some
import scala.collection.JavaConversions
import scala.collection.JavaConverters
import spock.lang.Specification
import spock.lang.Unroll

class MatchingSpec extends Specification {

  private static Request request

  def setup() {
    request = new Request('GET', '/', PactReaderKt.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: ['testreqheadervalue']], OptionalBody.body('{"test": true}'.bytes))
  }

  def 'Body Matching - Handle both None'() {
    expect:
    Matching.matchBody(new Request('', '', null, ['Content-Type': ['a']]),
      new Request('', '', null, ['Content-Type': ['a']]), true).isEmpty()
  }

  def 'Body Matching - Handle left None'() {
    expect:
    JavaConverters.asJavaCollection(
      Matching.matchBody(new Request('', '', null, ['Content-Type': ['a']], request.body),
      new Request('', '', null, ['Content-Type': ['a']]), true)).contains(mismatch)

    where:
    mismatch = new BodyMismatch(request.body.valueAsString(), null, 'Expected body \'{"test": true}\' but was missing')
  }

  def 'Body Matching - Handle right None'() {
    expect:
    Matching.matchBody(new Request('', '', null, ['Content-Type': ['a']]),
      new Request('', '', null, ['Content-Type': ['a']], request.body), true).isEmpty()
  }

  def 'Body Matching - Handle different mime types'() {
    expect:
    Matching.matchBody(new Request('', '', null, ['Content-Type': ['a']], request.body),
      new Request('', '', null, ['Content-Type': ['b']], request.body), true) == mismatch

    where:
    mismatch = JavaConversions.asScalaBuffer([ BodyTypeMismatch.apply('a', 'b') ]).toSeq()
  }

  def 'Body Matching - match different mimetypes by regexp'() {
    expect:
    Matching.matchBody(new Request('', '', null, ['Content-Type': ['application/x+json']], body),
      new Request('', '', null, ['Content-Type': ['application/x+json']], body), true).isEmpty()

    where:
    body = OptionalBody.body('{ "name":  "bob" }'.bytes)
  }

  @Unroll
  def 'Method matching - #desc'() {
    expect:
    Matching.matchMethod(valA, valB) == result

    where:

    desc                 | valA | valB | result
    'match same'         | 'a'  | 'a'  | None$.MODULE$
    'match ignore case'  | 'a'  | 'A'  | None$.MODULE$
    'mismatch different' | 'a'  | 'b'  | Some.apply(MethodMismatch.apply('a', 'b'))
  }

  private query(String queryString = '') {
    new Request('', '', PactReaderKt.queryStringToMap(queryString), null, OptionalBody.body(''.bytes), null)
  }

  def 'Query Matching - match same'() {
    expect:
    Matching.matchQuery(query('a=b'), query('a=b')).empty
  }

  def 'Query Matching - match none'() {
    expect:
    Matching.matchQuery(query(), query()).empty
  }

  def 'Query Matching - mismatch none to something'() {
    expect:
    Matching.matchQuery(query(), query('a=b')) == mismatch

    where:
    mismatch = JavaConversions.asScalaBuffer([ QueryMismatch.apply('a', '', 'b',
      Some.apply("Unexpected query parameter 'a' received"), '$.query.a') ]).toSeq()
  }

  def 'Query Matching - mismatch something to none'() {
    expect:
    Matching.matchQuery(query('a=b'), query()) == mismatch

    where:
    mismatch = JavaConversions.asScalaBuffer([ QueryMismatch.apply('a', 'b', '',
      Some.apply("Expected query parameter 'a' but was missing"), '$.query.a') ]).toSeq()
  }

  def 'Query Matching - match keys in different order'() {
    expect:
    Matching.matchQuery(query('status=RESPONSE_RECEIVED&insurerCode=ABC'),
      query('insurerCode=ABC&status=RESPONSE_RECEIVED')).empty
  }

  def 'Query Matching - mismatch if the same key is repeated with values in different order'() {
    expect:
    Matching.matchQuery(query('a=1&a=2&b=3'), query('a=2&a=1&b=3')) == mismatch

    where:
    mismatch = JavaConversions.asScalaBuffer([
      QueryMismatch.apply('a', '1', '2',
        Some.apply("Expected '1' but received '2' for query parameter 'a'"), 'a'),
      QueryMismatch.apply('a', '2', '1',
        Some.apply("Expected '2' but received '1' for query parameter 'a'"), 'a')
    ]).toSeq()
  }

}
