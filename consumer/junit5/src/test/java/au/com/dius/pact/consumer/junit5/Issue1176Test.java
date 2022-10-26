package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(PactConsumerTestExt.class)
public class Issue1176Test {
  private static final String CONFIG_URL = "/config";

  @BeforeEach
  public void setUp(MockServer mockServer) {
    assertNotNull(mockServer);
  }

  @Pact(provider = "config-service", consumer = "test-integration")
  public RequestResponsePact validCredentials(PactDslWithProvider builder) {
    Map<String, String> headers = Collections.singletonMap("Content-Type", ContentType.TEXT.toString());

    RequestResponsePact pact = builder
      .uponReceiving("valid configuration")
      .path(CONFIG_URL)
      .method("GET")
      .headers(headers)
      .body("text")
      .willRespondWith()
      .status(200)
      .body("{\"data\":\"\", \"status\":\"success\"}")
      .toPact();

    return pact;
  }

  @Test
  @PactTestFor(hostInterface = "localhost", pactMethod = "validCredentials", port = "7001", pactVersion = PactSpecVersion.V3)
  public void runTest(MockServer mockServer) {
    RequestLoggingFilter requestLoggingFilter = new RequestLoggingFilter();
    ResponseLoggingFilter responseLoggingFilter = new ResponseLoggingFilter();

    RequestSpecification requestSpec = new RequestSpecBuilder()
      .setContentType(ContentType.TEXT)
      .setPort(mockServer.getPort())
      .setBasePath(CONFIG_URL)
      .addFilter(requestLoggingFilter)
      .addFilter(responseLoggingFilter)
      .setBody("text")
      .build();

    Response response = given().spec(requestSpec).get();
    assertEquals("success", response.body().jsonPath().get("status"));
  }
}
