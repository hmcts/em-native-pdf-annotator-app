package uk.gov.hmcts.reform.em.npa.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FileTypeException extends RuntimeException {

    public FileTypeException(Exception e) {
        super(e);
    }

    public FileTypeException(String message) {
        super(message);
    }
}
