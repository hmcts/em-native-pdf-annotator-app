package uk.gov.hmcts.reform.em.npa.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StopWatch;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.em.npa.config.Constants;
import uk.gov.hmcts.reform.em.npa.service.DeleteService;
import uk.gov.hmcts.reform.em.npa.service.RedactionService;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;

import java.io.File;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

/**
 * REST controller for managing Redaction Requests.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Redactions Service", description = "Endpoint for managing Redactions.")
public class RedactionResource {

    private final Logger log = LoggerFactory.getLogger(RedactionResource.class);

    private RedactionService redactionService;
    private final DeleteService deleteService;
    @Value("${toggles.delete_enabled}")
    private boolean deleteEnabled;

    public RedactionResource(RedactionService redactionService, DeleteService deleteService) {
        this.redactionService = redactionService;
        this.deleteService = deleteService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields(Constants.IS_ADMIN);
    }

    @Operation(
        summary = "Burn markups onto Document",
        description = "A POST request to burn markups onto Document and return the newly redacted document",
        parameters = {
            @Parameter(
                in = ParameterIn.HEADER, name = "authorization",
                description = "Authorization (Idam Bearer token)", required = true,
                schema = @Schema(type = "string")),
            @Parameter(
                in = ParameterIn.HEADER, name = "serviceauthorization",
                description = "Service Authorization (S2S Bearer token)", required = true,
                schema = @Schema(type = "string"))}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully redacted"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorised"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Server Error"),
    })
    @PostMapping("/redaction")
    public ResponseEntity<Object> save(HttpServletRequest request,
                                       @RequestBody RedactionRequest redactionRequest) {
        try {
            String auth = request.getHeader("Authorization");
            String serviceAuth = request.getHeader("ServiceAuthorization");
            File newlyRedactedFile = redactionService.redactFile(
                    auth, serviceAuth, redactionRequest);

            HttpHeaders headers = new HttpHeaders();
            headers.add(CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", newlyRedactedFile.getName()));
            Tika tika = new Tika();

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(newlyRedactedFile.length())
                    .contentType(MediaType.parseMediaType(tika.detect(newlyRedactedFile)))
                    .body(new FileSystemResource(newlyRedactedFile));
        } catch (Exception e) {
            log.error("Failed redaction with error: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @Operation(
        summary = "Delete all redactions for a document",
        description = "Deletes all redactions associated with the provided DocumentId",
        parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "authorization",
                    schema = @Schema(type = "string"), required = true),
            @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                    schema = @Schema(type = "string"), required = true)
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorised"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "500", description = "Server Error")
    })
    @DeleteMapping("/redaction/document/{documentId}")
    public ResponseEntity<Void> deleteByDocumentId(
            @RequestHeader(value = "Authorization", required = true) String auth,
            @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuth,
            @PathVariable UUID documentId) {
        if (!deleteEnabled) {
            throw new AccessDeniedException("Delete endpoint is disabled");
        }
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.debug("REST request to delete all Redactions for documentId: {}", documentId);
        deleteService.deleteByDocumentId(documentId);
        stopWatch.stop();
        log.info("Delete redactions completed for document {} in {} ms", documentId, stopWatch.getTotalTimeMillis());
        return ResponseEntity.noContent().build();
    }
}
