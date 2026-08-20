package uk.gov.hmcts.reform.em.npa.rest.errors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.TestSecurityConfiguration;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the ExceptionTranslator controller advice.
 *
 * @see ExceptionTranslator
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
@ActiveProfiles("exception-test-controller-enabled")
class ExceptionTranslatorIntTest {

    @Autowired
    private ExceptionTranslatorTestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("NotNull.testDTO.fieldName", Locale.UK, "Field cannot be null");

        ExceptionTranslator exceptionTranslator = new ExceptionTranslator(messageSource);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionTranslator)
                .build();
    }

    @Test
    void testMethodArgumentTypeMismatchException() throws Exception {
        mockMvc.perform(get("/test/type-mismatch")
                        .param("id", "not-a-number")) // Triggers type mismatch
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testMethodArgumentNotValidWithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validation-failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.error").value("Default validation error"));
    }

    @Test
    void testMissingServletRequestParameterException() throws Exception {
        mockMvc.perform(get("/test/missing-param")) // Missing the 'requiredParam' argument
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail")
                        .value("problemDetail.org.springframework.web.bind.MissingServletRequestParameterException"));
    }

    @Test
    void testMethodNotSupportedException() throws Exception {
        mockMvc.perform(post("/test/missing-param")) // POSTing to a GET endpoint
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.detail")
                        .value("problemDetail.org.springframework.web.HttpRequestMethodNotSupportedException"))
                .andExpect(jsonPath("$.error").value("Request method 'POST' is not supported"));
    }
}
