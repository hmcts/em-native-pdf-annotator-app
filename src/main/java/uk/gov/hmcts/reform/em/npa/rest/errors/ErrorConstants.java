package uk.gov.hmcts.reform.em.npa.rest.errors;

import java.net.URI;

public final class ErrorConstants {

    public static final String ERR_CONCURRENCY_FAILURE = "error.concurrencyFailure";
    public static final String ERR_VALIDATION = "error.validation";
    public static final String ERR_FORBIDDEN = "error.http.403";
    public static final String ERR_UNAUTHORISED = "error.http.401";
    public static final String PROBLEM_BASE_URL = "https://npa/problem";
    public static final URI DEFAULT_TYPE = URI.create(PROBLEM_BASE_URL + "/problem-with-message");
    public static final URI CONSTRAINT_VIOLATION_TYPE = URI.create(PROBLEM_BASE_URL + "/constraint-violation");
    public static final URI PARAMETERIZED_TYPE = URI.create(PROBLEM_BASE_URL + "/parameterized");
    public static final URI ENTITY_NOT_FOUND_TYPE = URI.create(PROBLEM_BASE_URL + "/entity-not-found");
    public static final URI BAD_REQUEST = URI.create(PROBLEM_BASE_URL + "/bad-request");

    private ErrorConstants() {
    }
}
