package au.com.dius.pact.model.generators

import spock.lang.Specification

class DateGeneratorSpec extends Specification {

  def 'supports timezones'() {
    expect:
    new DateGenerator('yyyy-MM-ddZ', null).generate([:]) ==~ /\d{4}-\d{2}-\d{2}[-+]\d+/
  }

}
