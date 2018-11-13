package uk.gov.hmcts.reform.em.npa.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil

class NpaLoad extends Simulation {

  val t = new TestUtil
  object CreateAnnotation {

    val createAnnotation = exec(http("CreateAnnotation")
      .post("http://localhost:8080/api/document-tasks")
      .header("Content-Type", "application/json")
      .header("Authorization", t.getIdamToken("test@test.com"))
      .header("ServiceAuthorization", t.getS2sToken())
      .body(StringBody(
        """
          |{ "inputDocumentId": "7842fab6-4577-42e1-98ea-ae9ea87fa59a",
          |"outputDocumentId": ""7842fab6-4577-42e1-98ea-ae9ea87fa59a"
          |}""".stripMargin))
    )
  }

  val httpProtocol = http
    .header("Content-Type", "application/json")
  //  .proxy(
  //    Proxy("http://proxyout.reform.hmcts.net", 8080)
  //      .httpsPort(8080)
  //    )


  val scn = scenario("NPA load test").exec(CreateAnnotation.createAnnotation)

  setUp(scn.inject(atOnceUsers(1)).protocols(httpProtocol))
}

