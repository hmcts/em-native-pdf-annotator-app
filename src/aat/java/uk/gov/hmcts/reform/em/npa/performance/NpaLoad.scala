package uk.gov.hmcts.reform.em.npa.performance

import io.gatling.core.Predef._
import io.gatling.core.structure.{ScenarioBuilder}
import io.gatling.http.Predef._

import org.json.JSONObject
import uk.gov.hmcts.reform.em.npa.testutil.{Env, TestUtil}

class NpaLoad extends Simulation with HttpConfiguration {


  val testUtil = new TestUtil

  object Setup {

    def run = exec()
    val newDocId: String = testUtil.uploadDocument("one-page.pdf")
    val annotations = 20
    val pdfPages = 1

    val annotationSetId: String = testUtil.createAnnotationSetForDocumentId(newDocId)

    for (i <- 1 to annotations) {
      for (p <- 1 to pdfPages) {
        testUtil.saveAnnotation(annotationSetId, p)
      }
    }
  }

  object CreateTask {

    val newDocId = testUtil.getDocumentId
    val jsonObject = new JSONObject
    jsonObject.put("inputDocumentId", newDocId)
    println(newDocId)

    def createTask = exec(http("createTask")
      .post(Env.getTestUrl + "/api/document-tasks")
      .header("Authorization", testUtil.getIdamToken("test@test.com"))
      .header("ServiceAuthorization",  testUtil.getS2sToken())
      .body(StringBody(jsonObject.toString))
      .check(
        status.find.in(201),
        jsonPath("$.inputDocumentId").exists,
        jsonPath("$.outputDocumentId").exists,
        jsonPath("$.taskState").in("DONE")
      )
    )
  }

  val setup = scenario("setup").exec(Setup.run)
  val createClaimScenario: ScenarioBuilder = scenario("Create NPA task")
    .exec(
      CreateTask.createTask
    )

  setUp(setup.exec(createClaimScenario)
    .inject(atOnceUsers(1))
    .protocols(httpProtocol))
//    .maxDuration(10 minutes)
    .assertions(
      global.responseTime.max.lt(5000),
      forAll.failedRequests.count.lt(1)
    )
}

