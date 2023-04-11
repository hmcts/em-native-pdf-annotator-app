package uk.gov.hmcts.reform.em.npa.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
public class WelcomeResource {

    private static final String MESSAGE = "Welcome to Native PDF Annotator API!";

    /**
     * Root GET endpoint.
     *
     * <p>Azure application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on.
     * This is the endpoint to deal with that and therefore silence the unnecessary 404s as a response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping(
        path = "/",
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public ResponseEntity<Map<String, String>> welcome() {

        log.info("Welcome message : '{}'", MESSAGE);

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(Map.of("message",MESSAGE));
    }
}
