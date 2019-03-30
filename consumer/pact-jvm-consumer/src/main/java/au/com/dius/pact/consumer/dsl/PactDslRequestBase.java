package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.core.model.OptionalBody;
import au.com.dius.pact.core.model.generators.Generators;
import au.com.dius.pact.core.model.matchingrules.MatchingRules;
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl;
import au.com.dius.pact.core.model.matchingrules.RegexMatcher;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PactDslRequestBase {
  protected static final String CONTENT_TYPE = "Content-Type";
  /**
   * @deprecated Use Headers.MULTIPART_HEADER_REGEX
   */
  @Deprecated
  private static final String MULTIPART_HEADER_REGEX = "multipart/form-data;(\\s*charset=[^;]*;)?\\s*boundary=.*";

  protected final PactDslRequestWithoutPath defaultRequestValues;
  protected String requestMethod;
  protected Map<String, List<String>> requestHeaders = new HashMap<>();
  protected Map<String, List<String>> query = new HashMap<>();
  protected OptionalBody requestBody = OptionalBody.missing();
  protected MatchingRules requestMatchers = new MatchingRulesImpl();
  protected Generators requestGenerators = new Generators();

  public PactDslRequestBase(PactDslRequestWithoutPath defaultRequestValues) {
    this.defaultRequestValues = defaultRequestValues;
  }

  protected void setupDefaultValues() {
    if (defaultRequestValues != null) {
      if (StringUtils.isNotEmpty(defaultRequestValues.requestMethod)) {
        requestMethod = defaultRequestValues.requestMethod;
      }
      requestHeaders.putAll(defaultRequestValues.requestHeaders);
      query.putAll(defaultRequestValues.query);
      requestBody = defaultRequestValues.requestBody;
      requestMatchers = ((MatchingRulesImpl) defaultRequestValues.requestMatchers).copy();
      requestGenerators = new Generators(defaultRequestValues.requestGenerators.getCategories());
    }
  }

  protected void setupFileUpload(String partName, String fileName, String fileContentType, byte[] data) throws IOException {
    HttpEntity multipart = MultipartEntityBuilder.create()
      .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
      .addBinaryBody(partName, data, ContentType.create(fileContentType), fileName)
      .build();
    OutputStream os = new ByteArrayOutputStream();
    multipart.writeTo(os);

    requestBody = OptionalBody.body(os.toString().getBytes());
    requestMatchers.addCategory("header").addRule(CONTENT_TYPE, new RegexMatcher(MULTIPART_HEADER_REGEX,
      multipart.getContentType().getValue()));
    requestHeaders.put(CONTENT_TYPE, Collections.singletonList(multipart.getContentType().getValue()));
  }
}
