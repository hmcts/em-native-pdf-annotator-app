package uk.gov.hmcts.reform.em.npa.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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

import javax.servlet.http.HttpServletRequest;
import java.io.File;

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

    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.setDisallowedFields(Constants.IS_ADMIN);
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
