package uk.gov.hmcts.reform.em.npa.functional;


import io.restassured.response.Response;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@PropertySource(value = "classpath:application.yml")
@RunWith(SpringRunner.class)
@Ignore
public class OpenIdConnectScenarios {

  @Autowired
  private TestUtil testUtil;

  @Value("${test.url}")
  private String testUrl;

  private String idamAuth;
  private String s2sAuth;

  @Rule
  public ExpectedException exceptionThrown = ExpectedException.none();

    @Test
    public void testValidAuthenticationAndAuthorisation() throws IOException, InterruptedException {
        testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .request("GET", testUrl + "/api/document-tasks")
            .then()
            .statusCode(200) ;
    }

    // Invalid S2SAuth
    @Test
    public void testInvalidS2SAuth() throws IOException, InterruptedException {

        Response response = testUtil.invalidS2SAuth()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .request("GET", testUrl + "/api/document-tasks");

        assertThat(response.getStatusCode(), is(401));

    }

    //Invalid  IdamAuth
    @Test
    public void testWithInvalidIdamAuth() throws IOException, InterruptedException {

        Response response = testUtil.invalidIdamAuthrequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .request("GET", testUrl  + "/api/document-tasks");

        assertThat(response.getStatusCode(), is(401));

    }

  //Empty S2SAuth
  @Test
  public void testWithEmptyS2SAuth() throws IOException, InterruptedException {

        exceptionThrown.expect(IllegalArgumentException.class);

        Response response = testUtil.validAuthRequestWithEmptyS2SAuth()
           .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
           .request("GET", testUrl + "/api/document-tasks");

  }

  // Empty IdamAuth and Valid S2S Auth
  @Test
  public void testWithEmptyIdamAuthAndValidS2SAuth() throws IOException, InterruptedException {

        exceptionThrown.expect(IllegalArgumentException.class);

        final Response createTaskResponse = testUtil.validS2SAuthWithEmptyIdamAuth()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .request("GET", testUtil + "/api/document-tasks");

        exceptionThrown.expectMessage("Header value cannot be null");

  }

  // Empty IdamAuth and Empty S2SAuth
  @Test
  public void testIdamAuthAndS2SAuthAreEmpty() throws IOException, InterruptedException {

        exceptionThrown.expect(IllegalArgumentException.class);

        final Response createTaskResponse = testUtil.emptyIdamAuthAndEmptyS2SAuth()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .request("GET", testUtil + "/api/document-tasks");

  }

}
