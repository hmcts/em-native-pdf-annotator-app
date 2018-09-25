package uk.gov.hmcts.reform.em.npa.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.em.npa.batch.DocumentTaskItemProcessor;
import uk.gov.hmcts.reform.em.npa.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.npa.rest.errors.BadRequestAlertException;
import uk.gov.hmcts.reform.em.npa.rest.util.HeaderUtil;
import uk.gov.hmcts.reform.em.npa.rest.util.PaginationUtil;
import uk.gov.hmcts.reform.em.npa.service.DocumentTaskService;
import uk.gov.hmcts.reform.em.npa.service.dto.DocumentTaskDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing DocumentTask.
 */
@RestController
@RequestMapping("/api")
public class DocumentTaskResource {

    private final Logger log = LoggerFactory.getLogger(DocumentTaskResource.class);

    private static final String ENTITY_NAME = "documentTask";

    private final DocumentTaskService documentTaskService;

    public DocumentTaskResource(DocumentTaskService documentTaskService) {
        this.documentTaskService = documentTaskService;
    }

    /**
     * POST  /document-tasks : Create a new documentTask.
     *
     * @param documentTaskDTO the documentTaskDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new documentTaskDTO, or with status 400 (Bad Request) if the documentTask has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/document-tasks")
    ////@Timed
    public ResponseEntity<DocumentTaskDTO> createDocumentTask(@RequestBody DocumentTaskDTO documentTaskDTO, @RequestHeader("Authorization") String authorisationHeader) throws URISyntaxException {
        log.debug("REST request to save DocumentTask : {}", documentTaskDTO);
        if (documentTaskDTO.getId() != null) {
            throw new BadRequestAlertException("A new documentTask cannot already have an ID", ENTITY_NAME, "idexists");
        }
        documentTaskDTO.setJwt(authorisationHeader);
        documentTaskDTO.setTaskState(TaskState.NEW);
        DocumentTaskDTO result = documentTaskService.save(documentTaskDTO);

        return ResponseEntity.created(new URI("/api/document-tasks/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /document-tasks : Updates an existing documentTask.
     *
     * @param documentTaskDTO the documentTaskDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated documentTaskDTO,
     * or with status 400 (Bad Request) if the documentTaskDTO is not valid,
     * or with status 500 (Internal Server Error) if the documentTaskDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
//    @PutMapping("/document-tasks")
//    //@Timed
//    public ResponseEntity<DocumentTaskDTO> updateDocumentTask(@RequestBody DocumentTaskDTO documentTaskDTO) throws URISyntaxException {
//        log.debug("REST request to update DocumentTask : {}", documentTaskDTO);
//        if (documentTaskDTO.getId() == null) {
//            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
//        }
//        DocumentTaskDTO result = documentTaskService.save(documentTaskDTO);
//        return ResponseEntity.ok()
//            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, documentTaskDTO.getId().toString()))
//            .body(result);
//    }

    /**
     * GET  /document-tasks : get all the documentTasks.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of documentTasks in body
     */
    @GetMapping("/document-tasks")
    //@Timed
    public ResponseEntity<List<DocumentTaskDTO>> getAllDocumentTasks(Pageable pageable) {
        log.debug("REST request to get a page of DocumentTasks");
        Page<DocumentTaskDTO> page = documentTaskService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/document-tasks");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /document-tasks/:id : get the "id" documentTask.
     *
     * @param id the id of the documentTaskDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the documentTaskDTO, or with status 404 (Not Found)
     */
    @GetMapping("/document-tasks/{id}")
    //@Timed
    public ResponseEntity<DocumentTaskDTO> getDocumentTask(@PathVariable Long id) {
        log.debug("REST request to get DocumentTask : {}", id);
        Optional<DocumentTaskDTO> documentTaskDTO = documentTaskService.findOne(id);
        return ResponseUtil.wrapOrNotFound(documentTaskDTO);
    }

    /**
     * DELETE  /document-tasks/:id : delete the "id" documentTask.
     *
     * @param id the id of the documentTaskDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/document-tasks/{id}")
    //@Timed
    public ResponseEntity<Void> deleteDocumentTask(@PathVariable Long id) {
        log.debug("REST request to delete DocumentTask : {}", id);
        documentTaskService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
