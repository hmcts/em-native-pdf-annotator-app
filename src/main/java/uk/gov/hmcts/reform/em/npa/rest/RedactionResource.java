package uk.gov.hmcts.reform.em.npa.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.tika.Tika;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;
import uk.gov.hmcts.reform.em.npa.service.RedactionService;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

/**
 * REST controller for managing Redaction Requests.
 */
@RestController
@RequestMapping("/api")
public class RedactionResource {

    private RedactionService redactionService;

    public RedactionResource(RedactionService redactionService) {
        this.redactionService = redactionService;
    }

    @ApiOperation(value = "Burn markups onto Document", notes = "A POST request to burn markups onto Document and return the newly redacted document")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully redacted"),
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Server Error"),
    })
    @PostMapping("/redaction")
    public ResponseEntity save(HttpServletRequest request,
                                       @RequestBody RedactionRequest redactionRequest) {
        try {
            String jwt = request.getHeader("Authorization");
            File newlyRedactedFile = redactionService.redactFile(
                    jwt,
                    redactionRequest.getCaseId(),
                    redactionRequest.getDocumentId(),
                    redactionRequest.getRedactions());

            InputStreamResource resource = new InputStreamResource(new FileInputStream(newlyRedactedFile));
            HttpHeaders headers = new HttpHeaders();
            headers.add(CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", newlyRedactedFile.getName()));
            Tika tika = new Tika();

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(newlyRedactedFile.length())
                    .contentType(MediaType.parseMediaType(tika.detect(newlyRedactedFile)))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}
