package uk.gov.hmcts.reform.em.npa.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.em.npa.config.Constants;
import uk.gov.hmcts.reform.em.npa.rest.errors.EmptyResponseException;
import uk.gov.hmcts.reform.em.npa.rest.errors.ValidationErrorException;
import uk.gov.hmcts.reform.em.npa.rest.util.HeaderUtil;
import uk.gov.hmcts.reform.em.npa.rest.util.PaginationUtil;
import uk.gov.hmcts.reform.em.npa.service.MarkUpService;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing Redaction MarkUps.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Markups Service", description = "Endpoint for managing Markups.")
public class MarkUpResource {

    private final Logger log = LoggerFactory.getLogger(MarkUpResource.class);

    private static final String ENTITY_NAME = "redaction";

    private MarkUpService markUpService;

    public MarkUpResource(MarkUpService markUpService){
        this.markUpService = markUpService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.setDisallowedFields(Constants.IS_ADMIN);
    }

    /**
     * POST  /markups : Create a new markup.
     *
     * @param redactionDTO the RedactionDTO to create
     * @return the ResponseEntity with status "201" (Created) and with body the new RedactionDTO
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @Operation(summary = "Create an RedactionDTO", description = "A POST request to create an RedactionDTO",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "authorization",
                            description = "Authorization (Idam Bearer token)", required = true,
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                            description = "Service Authorization (S2S Bearer token)", required = true,
                            schema = @Schema(type = "string"))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created"),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
    })
    @PostMapping("/markups")
    public ResponseEntity<RedactionDTO> createMarkUp(@Valid @RequestBody RedactionDTO redactionDTO,
                                                     BindingResult result) throws URISyntaxException {

        log.debug("REST request to save Redaction : {}", redactionDTO);

        if (result.hasErrors()) {
            throw new ValidationErrorException(result.getFieldErrors().stream()
                    .map(fe -> String.format("%s - %s", fe.getField(), fe.getCode()))
                    .collect(Collectors.joining(",")));
        }

        RedactionDTO response = markUpService.save(redactionDTO);
        return ResponseEntity.created(new URI("/api/markups/" + response.getRedactionId()))
                .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, response.getRedactionId().toString()))
                .body(response);
    }


    /**
     * PUT  /markups : Updates an existing markup.
     *
     * @param redactionDTO the RedactionDTO to update
     * @return the ResponseEntity with status "200" (OK) and with body the updated RedactionDTO,
     * or with status "500" (Internal Server Error) if the markup couldn't be updated
     */
    @Operation(summary = "Update an existing RedactionDTO", description = "A PUT request to update an RedactionDTO",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "authorization",
                            description = "Authorization (Idam Bearer token)", required = true,
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                            description = "Service Authorization (S2S Bearer token)", required = true,
                            schema = @Schema(type = "string"))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "RedactionDTO couldn't be updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
    })
    @PutMapping("/markups")
    public ResponseEntity<RedactionDTO> updateMarkUp(@Valid @RequestBody RedactionDTO redactionDTO,
                                                     BindingResult result) {
        log.debug("REST request to update Redaction : {}", redactionDTO);

        if (result.hasErrors()) {
            throw new ValidationErrorException(result.getFieldErrors().stream()
                    .map(fe -> String.format("%s - %s", fe.getField(), fe.getCode()))
                    .collect(Collectors.joining(",")));
        }

        RedactionDTO response = markUpService.save(redactionDTO);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, response.getRedactionId().toString()))
                .body(response);
    }

    /**
     * GET  /markups/:documentId : get all the markups for a specific document.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status "200" (OK) and the list of markups in body
     */
    @Operation(summary = "Get all markups for Document ID",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "authorization",
                            description = "Authorization (Idam Bearer token)", required = true,
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                            description = "Service Authorization (S2S Bearer token)", required = true,
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.PATH, name = "documentId",
                            description = "Document Id", required = true,
                            schema = @Schema(type = "UUID"))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
    })
    @GetMapping("/markups/{documentId}")
    public ResponseEntity<List<RedactionDTO>> getAllDocumentMarkUps(@PathVariable UUID documentId, Pageable pageable) {
        log.debug("REST request to get a page of markups");
        Page<RedactionDTO> page = markUpService.findAllByDocumentId(documentId, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/markups");
        if (page.hasContent()) {
            return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
        } else {
            throw new EmptyResponseException("Could not find markups for this document id#" + documentId);
        }
    }

    /**
     * DELETE  /markups/:documentId : delete all the markups of the document.
     *
     * @param documentId the id of the Document
     * @return the ResponseEntity with status "204" (No Content)
     */
    @Operation(summary = "Delete all RedactionDTOs", description = "A DELETE request to delete all the markups of the document",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "authorization",
                            description = "Authorization (Idam Bearer token)", required = true,
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                            description = "Service Authorization (S2S Bearer token)", required = true,
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.PATH, name = "documentId",
                            description = "Document Id", required = true,
                            schema = @Schema(type = "UUID"))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
    })
    @DeleteMapping("/markups/{documentId}")
    public ResponseEntity<Void> deleteMarkUps(@PathVariable UUID documentId) {
        log.debug("REST request to delete all Redactions for entire document : {}", documentId);
        markUpService.deleteAll(documentId);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, documentId.toString())).build();
    }

    /**
     * DELETE  /markups/:documentId/:redactionId : delete the markup of the document.
     *
     * @param documentId the id of the Document
     *
     * @param redactionId the id of the RedactionDTO
     * @return the ResponseEntity with status "200" (OK)
     */
    @Operation(summary = "Delete a RedactionDTO", description = "A DELETE request to delete the markup of the document",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "authorization",
                            description = "Authorization (Idam Bearer token)", required = true,
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.HEADER, name = "serviceauthorization",
                            description = "Service Authorization (S2S Bearer token)", required = true,
                            schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.PATH, name = "documentId",
                            description = "Document Id", required = true,
                            schema = @Schema(type = "UUID")),
                    @Parameter(in = ParameterIn.PATH, name = "redactionId",
                            description = "Redaction Id", required = true,
                            schema = @Schema(type = "UUID"))})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "401", description = "Unauthorised"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
    })
    @DeleteMapping("/markups/{documentId}/{redactionId}")
    public ResponseEntity<Void> deleteMarkUp(@PathVariable UUID documentId, @PathVariable UUID redactionId) {
        log.debug("REST request to delete a Redaction of the document : {}", documentId);
        markUpService.delete(redactionId);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, redactionId.toString())).build();
    }
}
