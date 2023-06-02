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
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.em.npa.config.Constants;
import uk.gov.hmcts.reform.em.npa.service.RedactionService;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;

import java.io.File;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

/**
 * REST controller for managing Redaction Requests.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Redactions Service", description = "Endpoint for managing Redactions.")
public class RedactionResource {

    private RedactionService redactionService;

    public RedactionResource(RedactionService redactionService) {
        this.redactionService = redactionService;
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
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}
