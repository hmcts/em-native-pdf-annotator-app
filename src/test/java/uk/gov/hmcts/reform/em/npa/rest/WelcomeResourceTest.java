package uk.gov.hmcts.reform.em.npa.rest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WelcomeResourceTest {

    private final WelcomeResource welcomeResource = new WelcomeResource();

    @Test
    void test_should_return_welcome_response() {

        ResponseEntity<Map<String, String>> responseEntity = welcomeResource.welcome();
        String expectedMessage = "Welcome to Native PDF Annotator API!";

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertThat(responseEntity.getBody().get("message")).contains(expectedMessage);
    }
}
