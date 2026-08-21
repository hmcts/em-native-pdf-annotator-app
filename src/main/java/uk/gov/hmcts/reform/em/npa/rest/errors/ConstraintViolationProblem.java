package uk.gov.hmcts.reform.em.npa.rest.errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.List;

import static uk.gov.hmcts.reform.em.npa.rest.errors.ErrorConstants.DEFAULT_TYPE;

public class ConstraintViolationProblem {

    private final ProblemDetail problemDetail;

    public ConstraintViolationProblem(HttpStatus status, List<Violation> violations) {
        this.problemDetail = ProblemDetail.forStatusAndDetail(status, "Constraint Violation");
        this.problemDetail.setType(DEFAULT_TYPE);
        this.problemDetail.setProperty("message", ErrorConstants.ERR_VALIDATION);
        this.problemDetail.setProperty("violations", violations);
    }

    public ProblemDetail getProblemDetail() {
        return this.problemDetail;
    }
}