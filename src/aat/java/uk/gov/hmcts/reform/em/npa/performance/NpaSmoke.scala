package uk.gov.hmcts.reform.em.npa.performance
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class NpaSmoke extends Simulation {

    object Health {

      val health = exec(http("health")
        .get("http://localhost:8080/health"))
    }

    val httpProtocol = http
      .header("Content-Type", "application/json")
    //  .proxy(
    //    Proxy("http://proxyout.reform.hmcts.net", 8080)
    //      .httpsPort(8080)
    //    )


    val scn = scenario("NPA smoke test").exec(Health.health)

    setUp(scn.inject(atOnceUsers(1)).protocols(httpProtocol))
}

