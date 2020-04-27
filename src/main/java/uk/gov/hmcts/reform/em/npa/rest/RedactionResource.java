package uk.gov.hmcts.reform.em.npa.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.em.npa.domain.RedactionRequest;
import uk.gov.hmcts.reform.em.npa.service.RedactionService;

import javax.servlet.http.HttpServletRequest;

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

    @ApiOperation(value = "Burn markups onto Document", notes = "A POST request to burn markups onto Document")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully redacted", response = String.class),
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
    })
    @PostMapping("/redaction")
    public ResponseEntity<String> save(HttpServletRequest request,
                                       @RequestBody RedactionRequest redactionRequest) {

        String jwt = request.getHeader("authorization");
        return ResponseEntity.ok(
                redactionService.redactFile(
                    jwt,
                    redactionRequest.getCaseId(),
                    redactionRequest.getDocumentId(),
                    redactionRequest.getMarkups()
                )
        );
    }
}
