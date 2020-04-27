package uk.gov.hmcts.reform.em.npa.ccd.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CaseDocumentNotFoundException extends RuntimeException {
}
