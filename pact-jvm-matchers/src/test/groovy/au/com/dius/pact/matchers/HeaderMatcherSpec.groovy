package au.com.dius.pact.matchers

import au.com.dius.pact.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.model.matchingrules.RegexMatcher
import au.com.dius.pact.model.matchingrules.RuleLogic
import spock.lang.Specification
import spock.lang.Unroll

class HeaderMatcherSpec extends Specification {

  def "matching headers - be true when headers are equal"() {
    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER', 'HEADER',
      new MatchingRulesImpl()) == null
  }

  def "matching headers - be false when headers are not equal"() {
    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER', 'HEADSER',
      new MatchingRulesImpl()) != null
  }

  def "matching headers - exclude whitespace from the comparison"() {
    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER1, HEADER2,   3', 'HEADER1,HEADER2,3',
      new MatchingRulesImpl()) == null
  }

  def "matching headers - delegate to a matcher when one is defined"() {
    given:
    def matchers = new MatchingRulesImpl()
    matchers.addCategory('header').addRule('HEADER', new RegexMatcher('.*'))

    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER', 'XYZ', matchers) == null
  }

  def "matching headers - combines mismatches if there are multiple"() {
    given:
    def matchers = new MatchingRulesImpl()
    def category = matchers.addCategory('header')
    category.addRule('HEADER', new RegexMatcher('X=.*'), RuleLogic.OR)
    category.addRule('HEADER', new RegexMatcher('A=.*'), RuleLogic.OR)
    category.addRule('HEADER', new RegexMatcher('B=.*'), RuleLogic.OR)

    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER', 'XYZ', matchers).mismatch ==
      "Expected 'XYZ' to match 'X=.*', Expected 'XYZ' to match 'A=.*', Expected 'XYZ' to match 'B=.*'"
  }

  @Unroll
  @SuppressWarnings('LineLength')
  def "matching headers - content type header - be true when #description"() {
    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', expected, actual, new MatchingRulesImpl()) == null

    where:

    description                                       | expected                         | actual
    'headers are equal'                               | 'application/json;charset=UTF-8' | 'application/json; charset=UTF-8'
    'headers are equal but have different case'       | 'application/json;charset=UTF-8' | 'application/JSON; charset=utf-8'
    'the charset is missing from the expected header' | 'application/json'               | 'application/json ; charset=utf-8'
  }

  def "matching headers - content type header - be false when headers are not equal"() {
    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json;charset=UTF-8',
      'application/pdf;charset=UTF-8', new MatchingRulesImpl()) != null
  }

  def "matching headers - content type header - be false when charsets are not equal"() {
    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json;charset=UTF-8',
      'application/json;charset=UTF-16', new MatchingRulesImpl()) != null
  }

  def "matching headers - content type header - be false when other parameters are not equal"() {
    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json;declaration="<950118.AEB0@XIson.com>"',
      'application/json;charset=UTF-8', new MatchingRulesImpl()) != null
  }

  def "matching headers - content type header - delegate to any defined matcher"() {
    given:
    def matchers = new MatchingRulesImpl()
    matchers.addCategory('header').addRule('CONTENT-TYPE', new RegexMatcher('[a-z]+\\/[a-z]+'))

    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json',
      'application/json;charset=UTF-8', matchers) != null
    HeaderMatcher.compareHeader('content-type', 'application/json',
      'application/json;charset=UTF-8', matchers) != null
    HeaderMatcher.compareHeader('Content-Type', 'application/json',
      'application/json;charset=UTF-8', matchers) != null
  }

  def "parse parameters - parse the parameters into a map"() {
    expect:
    HeaderMatcher.parseParameters(['A=B']) == [A: 'B']
    HeaderMatcher.parseParameters(['A=B', 'C=D']) == [A: 'B', C: 'D']
    HeaderMatcher.parseParameters(['A= B', 'C =D ']) == [A: 'B', C: 'D']
  }

  @Unroll
  def 'strip whitespace test'() {
    expect:
    HeaderMatcher.INSTANCE.stripWhiteSpaceAfterCommas(str) == expected

    where:

    str         | expected
    ''          | ''
    ' '         | ' '
    'abc'       | 'abc'
    'abc xyz'   | 'abc xyz'
    'abc,xyz'   | 'abc,xyz'
    'abc, xyz'  | 'abc,xyz'
    'abc , xyz' | 'abc ,xyz'
  }

}
