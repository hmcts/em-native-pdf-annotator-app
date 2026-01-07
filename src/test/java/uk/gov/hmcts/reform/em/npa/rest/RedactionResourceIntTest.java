package uk.gov.hmcts.reform.em.npa.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.TestSecurityConfiguration;
import uk.gov.hmcts.reform.em.npa.rest.errors.ExceptionTranslator;
import uk.gov.hmcts.reform.em.npa.service.DeleteService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for RedactionResource delete endpoint.
 * Tests HTTP status codes and exception translation.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
class RedactionResourceIntTest {

    @Autowired
    private MarkUpResource markUpResource;

    @MockBean
    private DeleteService deleteService;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(markUpResource)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter)
            .build();
    }

    @Test
    void shouldReturn204WhenDeleteSucceeds() throws Exception {
        ReflectionTestUtils.setField(markUpResource, "deleteEnabled", true);
        UUID documentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/markups/document/{documentId}", documentId)
                .header("Authorization", "Bearer jwt")
                .header("ServiceAuthorization", "Bearer s2s"))
            .andExpect(status().isNoContent());

        verify(deleteService).deleteByDocumentId(documentId);
    }

    @Test
    void shouldReturn403WhenDeleteDisabled() throws Exception {
        ReflectionTestUtils.setField(markUpResource, "deleteEnabled", false);
        UUID documentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/markups/document/{documentId}", documentId)
                .header("Authorization", "Bearer jwt")
                .header("ServiceAuthorization", "Bearer s2s"))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verify(deleteService, never()).deleteByDocumentId(any());
    }

    @Test
    void shouldReturn400WhenInvalidUuidFormat() throws Exception {
        ReflectionTestUtils.setField(markUpResource, "deleteEnabled", true);

        mockMvc.perform(delete("/api/markups/document/{documentId}", "invalid-uuid")
                .header("Authorization", "Bearer jwt")
                .header("ServiceAuthorization", "Bearer s2s"))
            .andExpect(status().isBadRequest());

        verify(deleteService, never()).deleteByDocumentId(any());
    }

    @Test
    void shouldReturn500WhenDeleteThrowsRuntimeException() throws Exception {
        ReflectionTestUtils.setField(markUpResource, "deleteEnabled", true);
        UUID documentId = UUID.randomUUID();
        doThrow(new RuntimeException("Database error")).when(deleteService).deleteByDocumentId(any());

        mockMvc.perform(delete("/api/markups/document/{documentId}", documentId)
                .header("Authorization", "Bearer jwt")
                .header("ServiceAuthorization", "Bearer s2s"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verify(deleteService).deleteByDocumentId(documentId);
    }

    @Test
    void shouldReturn204WhenNoRecordsToDelete() throws Exception {
        ReflectionTestUtils.setField(markUpResource, "deleteEnabled", true);
        UUID documentId = UUID.randomUUID();
        // deleteService.deleteByDocumentId does nothing (no records found) - default mock behavior

        mockMvc.perform(delete("/api/markups/document/{documentId}", documentId)
                .header("Authorization", "Bearer jwt")
                .header("ServiceAuthorization", "Bearer s2s"))
            .andExpect(status().isNoContent());

        verify(deleteService).deleteByDocumentId(documentId);
    }
}
