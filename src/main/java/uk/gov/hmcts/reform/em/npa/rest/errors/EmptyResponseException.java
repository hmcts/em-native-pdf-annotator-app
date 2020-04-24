package uk.gov.hmcts.reform.em.npa.rest.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NO_CONTENT)
public class EmptyResponseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmptyResponseException(String msg) {
        super(msg);
    }
}
