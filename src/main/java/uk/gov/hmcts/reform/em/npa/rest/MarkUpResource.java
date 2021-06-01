package uk.gov.hmcts.reform.em.npa.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
        binder.setDisallowedFields("isAdmin");
    }

    /**
     * POST  /markups : Create a new markup.
     *
     * @param redactionDTO the RedactionDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new RedactionDTO
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @ApiOperation(value = "Create an RedactionDTO", notes = "A POST request to create an RedactionDTO")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created", response = RedactionDTO.class),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
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
     * @return the ResponseEntity with status 200 (OK) and with body the updated RedactionDTO,
     * or with status 500 (Internal Server Error) if the markup couldn't be updated
     */
    @ApiOperation(value = "Update an existing RedactionDTO", notes = "A PUT request to update an RedactionDTO")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = RedactionDTO.class),
            @ApiResponse(code = 500, message = "RedactionDTO couldn't be updated"),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not Found"),
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
     * @return the ResponseEntity with status 200 (OK) and the list of markups in body
     */
    @ApiOperation(value = "Get all markups for Document ID")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = RedactionDTO.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not Found"),
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
     * @return the ResponseEntity with status 200 (OK)
     */
    @ApiOperation(value = "Delete all RedactionDTOs", notes = "A DELETE request to delete all the markups of the document")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not Found"),
    })
    @DeleteMapping("/markups/{documentId}")
    public ResponseEntity<Void> deleteMarkUps(@PathVariable UUID documentId) {
        log.debug("REST request to delete all Redactions for entire document : {}", documentId);
        markUpService.deleteAll(documentId);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, documentId.toString())).build();
    }

    /**
     * DELETE  /markups/:documentId/:redactionId : delete the markup of the document.
     *
     * @param documentId the id of the Document
     *
     * @param redactionId the id of the RedactionDTO
     * @return the ResponseEntity with status 200 (OK)
     */
    @ApiOperation(value = "Delete a RedactionDTO", notes = "A DELETE request to delete the markup of the document")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 401, message = "Unauthorised"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "Not Found"),
    })
    @DeleteMapping("/markups/{documentId}/{redactionId}")
    public ResponseEntity<Void> deleteMarkUp(@PathVariable UUID documentId, @PathVariable UUID redactionId) {
        log.debug("REST request to delete a Redaction of the document : {}", documentId);
        markUpService.delete(redactionId);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, redactionId.toString())).build();
    }
}
