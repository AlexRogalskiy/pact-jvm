package au.com.dius.pact.provider.junit5

import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import com.github.tomakehurst.wiremock.WireMockServer
import groovy.util.logging.Slf4j
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver

import static com.github.tomakehurst.wiremock.client.WireMock.*

@Provider('ProviderWithSlashes')
@PactFolder('pacts')
@ExtendWith([
  WiremockResolver,
  WiremockUriResolver
])
@Slf4j
class BodyWithSlashesTest {

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider)
  void testTemplate(PactVerificationContext context) {
    context.verifyInteraction()
  }

  @BeforeEach
  void before(PactVerificationContext context, @WiremockResolver.Wiremock WireMockServer server,
              @WiremockUriResolver.WiremockUri String uri) throws MalformedURLException {
    context.setTarget(HttpTestTarget.fromUrl(new URL(uri)))

    server.stubFor(
      get(urlPathEqualTo('/shipping/v1'))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader('Content-Type', 'application/json')
          .withBody('{ "data": [ { "relationships": { "user/shippingAddress": { "data": { "id": "123", "type": "user/shipping-address" } } } } ] }'))
    )
  }
}
