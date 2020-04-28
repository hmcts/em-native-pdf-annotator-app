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
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.MarkUpDTO;
import uk.gov.hmcts.reform.em.npa.rest.errors.EmptyResponseException;
import uk.gov.hmcts.reform.em.npa.rest.errors.ValidationErrorException;
import uk.gov.hmcts.reform.em.npa.rest.util.HeaderUtil;
import uk.gov.hmcts.reform.em.npa.rest.util.PaginationUtil;
import uk.gov.hmcts.reform.em.npa.service.MarkUpService;

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

    private static final String ENTITY_NAME = "markup";

    private MarkUpService markUpService;

    public MarkUpResource(MarkUpService markUpService){
        this.markUpService = markUpService;
    }

    /**
     * POST  /markups : Create a new markup.
     *
     * @param markUpDTO the MarkUpDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new MarkUpDTO
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @ApiOperation(value = "Create an MarkUpDTO", notes = "A POST request to create an MarkUpDTO")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Successfully created", response = MarkUpDTO.class),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
    })
    @PostMapping("/markups")
    public ResponseEntity<MarkUpDTO> createMarkUp(@Valid @RequestBody MarkUpDTO markUpDTO,
                                                     BindingResult result) throws URISyntaxException {

        log.debug("REST request to save MarkUp : {}", markUpDTO);

        if (result.hasErrors()) {
            throw new ValidationErrorException(result.getFieldErrors().stream()
                    .map(fe -> String.format("%s - %s", fe.getField(), fe.getCode()))
                    .collect(Collectors.joining(",")));
        }

        MarkUpDTO response = markUpService.save(markUpDTO);
        return ResponseEntity.created(new URI("/api/markups/" + response.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, response.getId().toString()))
                .body(response);
    }


    /**
     * PUT  /markups : Updates an existing markup.
     *
     * @param markUpDTO the MarkUpDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated MarkUpDTO,
     * or with status 500 (Internal Server Error) if the markup couldn't be updated
     */
    @ApiOperation(value = "Update an existing MarkUpDTO", notes = "A PUT request to update an MarkUpDTO")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = MarkUpDTO.class),
            @ApiResponse(code = 500, message = "MarkUpDTO couldn't be updated"),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not Found"),
    })
    @PutMapping("/markups")
    public ResponseEntity<MarkUpDTO> updateMarkUp(@Valid @RequestBody MarkUpDTO markUpDTO,
                                                     BindingResult result) {
        log.debug("REST request to update MarkUp : {}", markUpDTO);

        if (result.hasErrors()) {
            throw new ValidationErrorException(result.getFieldErrors().stream()
                    .map(fe -> String.format("%s - %s", fe.getField(), fe.getCode()))
                    .collect(Collectors.joining(",")));
        }

        MarkUpDTO response = markUpService.save(markUpDTO);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, markUpDTO.getId().toString()))
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
            @ApiResponse(code = 200, message = "Success", response = MarkUpDTO.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not Found"),
    })
    @GetMapping("/markups/{documentId}")
    public ResponseEntity<List<MarkUpDTO>> getAllDocumentMarkUps(@PathVariable UUID documentId, Pageable pageable) {
        log.debug("REST request to get a page of markups");
        Page<MarkUpDTO> page = markUpService.findAllByDocumentId(documentId, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/markups");
        if (page.hasContent()) {
            return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
        } else {
            throw new EmptyResponseException("Could not find markups for this document id#" + documentId);
        }
    }

    /**
     * DELETE  /markups/:id : delete the "id" markup.
     *
     * @param id the id of the MarkUpDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @ApiOperation(value = "Delete a MarkUpDTO", notes = "A DELETE request to delete a MarkUpDTO")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 401, message = "Unauthorised"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not Found"),
    })
    @DeleteMapping("/markups/{id}")
    public ResponseEntity<Void> deleteMarkUp(@PathVariable UUID id) {
        log.debug("REST request to delete MarkUp : {}", id);
        markUpService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

}
