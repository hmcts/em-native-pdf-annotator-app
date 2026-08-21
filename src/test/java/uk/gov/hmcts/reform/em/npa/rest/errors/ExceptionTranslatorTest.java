package uk.gov.hmcts.reform.em.npa.rest.errors;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExceptionTranslatorTest {

    private static final Logger log = LoggerFactory.getLogger(ExceptionTranslatorTest.class);

    @Mock
    private NativeWebRequest request;

    @Mock
    private WebRequest webRequest;

    @Mock
    private NativeWebRequest nativeWebRequest;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private ExceptionTranslator exceptionTranslator;


    private void setupRequestMocks() {
        when(request.getNativeRequest(HttpServletRequest.class)).thenReturn(httpServletRequest);
        when(httpServletRequest.getRequestURI()).thenReturn("/test/path");
    }

    @Test
    void shouldHandleNoSuchElementException() {
        NoSuchElementException exception = new NoSuchElementException("Entity not found");

        ResponseEntity<Object> response = exceptionTranslator
                .handleNoSuchElementException(exception, nativeWebRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getBody()).isNotNull();

        ProblemDetail problemDetail = (ProblemDetail) response.getBody();

        assertThat(problemDetail.getProperties())
            .containsEntry("message", ErrorConstants.ENTITY_NOT_FOUND_TYPE);
    }

    @Test
    void shouldHandleBadRequestAlertException() {
        BadRequestAlertException exception = new BadRequestAlertException(
            "Bad request",
            "entityName",
            "errorKey"
        );

        ResponseEntity<Object> response = exceptionTranslator
                .handleBadRequestAlertException(exception, nativeWebRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody()).isNotNull();

        ProblemDetail problemDetail = (ProblemDetail) response.getBody();
        assertThat(problemDetail.getProperties())
            .containsEntry("message", ErrorConstants.BAD_REQUEST);
    }

    @Test
    void shouldHandleConcurrencyFailureException() {
        ConcurrencyFailureException exception = new ConcurrencyFailureException("Concurrency failure");

        ResponseEntity<Object> response = exceptionTranslator
                .handleConcurrencyFailure(exception, nativeWebRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getBody()).isNotNull();

        ProblemDetail problemDetail = (ProblemDetail) response.getBody();
        assertThat(problemDetail.getProperties())
            .containsEntry("message", ErrorConstants.ERR_CONCURRENCY_FAILURE);
    }

    @Test
    void shouldHandleAccessDeniedException() {
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        ResponseEntity<Object> response = exceptionTranslator
                .handleAccessDenied(exception, nativeWebRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getBody()).isNotNull();

        ProblemDetail problemDetail = (ProblemDetail) response.getBody();
        assertThat(problemDetail.getProperties())
            .containsEntry("message", ErrorConstants.ERR_FORBIDDEN);
    }

    @Test
    void shouldHandleBadCredentialsException() {
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        ResponseEntity<Object> response = exceptionTranslator
                .handleUnAuthorised(exception, nativeWebRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getBody()).isNotNull();

        ProblemDetail problemDetail = (ProblemDetail) response.getBody();
        assertThat(problemDetail.getProperties())
            .containsEntry("message", ErrorConstants.ERR_UNAUTHORISED);
    }

    @Test
    void shouldHandleMethodArgumentNotValidException() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        
        FieldError fieldError1 = new FieldError("testObject", "field1", null, false, 
            new String[]{"NotNull"}, null, "must not be null");
        FieldError fieldError2 = new FieldError("testObject", "field2", null, false, 
            new String[]{"NotBlank"}, null, "must not be blank");
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        ResponseEntity<Object> response = exceptionTranslator
                .handleMethodArgumentNotValid(exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

        log.info("Response Body: {}", response.getBody());

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody()).isNotNull();
        ProblemDetail problemDetail = (ProblemDetail) response.getBody();

        assertThat(problemDetail.getType()).isEqualTo(ErrorConstants.CONSTRAINT_VIOLATION_TYPE);
        assertThat(problemDetail.getTitle()).isEqualTo("Method argument not valid");
        assertThat(problemDetail.getProperties())
            .containsEntry("message", ErrorConstants.ERR_VALIDATION);

        List<FieldErrorVM> fieldErrors = (List<FieldErrorVM>) problemDetail.getProperties().get("fieldErrors");
        assertThat(fieldErrors).hasSize(2);
        assertThat(fieldErrors.get(0).getField()).isEqualTo("field1");
        assertThat(fieldErrors.get(0).getMessage()).isEqualTo("NotNull");
        assertThat(fieldErrors.get(1).getField()).isEqualTo("field2");
        assertThat(fieldErrors.get(1).getMessage()).isEqualTo("NotBlank");
    }

    @Test
    void shouldProcessConstraintViolationProblem() {
        setupRequestMocks();
        Violation violation = new Violation("field", "must not be null");
        ConstraintViolationProblem problem = new ConstraintViolationProblem(
                HttpStatus.BAD_REQUEST,
            List.of(violation)
        );
        ResponseEntity<ProblemDetail> entity = ResponseEntity.badRequest().body(problem.getProblemDetail());

        ResponseEntity<ProblemDetail> result = exceptionTranslator.process(entity, request);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getBody().getProperties())
            .containsEntry("message", ErrorConstants.ERR_VALIDATION)
            .containsKey("violations")
            .containsKey("path");
        assertThat(result.getBody().getProperties().get("path")).isEqualTo("/test/path");
    }

    @Test
    void shouldProcessDefaultProblem() {
        setupRequestMocks();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,"Test detail");
        problemDetail.setTitle("Test Problem");
        problemDetail.setType(URI.create("https://example.com/problem"));
        problemDetail.setProperty("customParam", "customValue");

        ResponseEntity<ProblemDetail> entity =
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);

        ResponseEntity<ProblemDetail> result = exceptionTranslator.process(entity, request);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getType()).isEqualTo(URI.create("https://example.com/problem"));
        assertThat(result.getBody().getTitle()).isEqualTo("Test Problem");
        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getBody().getDetail()).isEqualTo("Test detail");
        assertThat(result.getBody().getProperties())
            .containsEntry("customParam", "customValue")
            .containsEntry("message", "error.http.500")
            .containsEntry("path", "/test/path");
    }

    @Test
    void shouldProcessDefaultProblemWithDefaultType() {
        setupRequestMocks();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Test detail");
        problemDetail.setTitle("Default Problem");
        problemDetail.setType(URI.create("https://example.com/problem"));

        ResponseEntity<ProblemDetail> entity = ResponseEntity.badRequest().body(problemDetail);

        ResponseEntity<ProblemDetail> result = exceptionTranslator.process(entity, request);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getType()).isEqualTo(URI.create("https://example.com/problem"));
        assertThat(result.getBody().getTitle()).isEqualTo("Default Problem");
        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());

    }

    @Test
    void shouldProcessDefaultProblemWithExistingMessageParameter() {
        setupRequestMocks();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Test detail");
        problemDetail.setTitle("Test Problem");
        problemDetail.setType(URI.create("https://example.com/problem"));
        problemDetail.setProperty("message", "custom.error.message");

        ResponseEntity<ProblemDetail> entity = ResponseEntity.badRequest().body(problemDetail);

        ResponseEntity<ProblemDetail> result = exceptionTranslator.process(entity, request);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getBody().getProperties())
            .containsEntry("message", "custom.error.message");
    }

    @Test
    void shouldProcessProblemWithNullRequest() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Test detail");
        problemDetail.setTitle("Test Problem");
        problemDetail.setType(URI.create("https://example.com/problem"));
        ResponseEntity<ProblemDetail> entity = ResponseEntity.badRequest().body(problemDetail);

        ResponseEntity<ProblemDetail> result = exceptionTranslator.process(entity, request);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getProperties()).doesNotContainKey("path");
    }

    @Test
    void shouldProcessProblemWithNullHttpServletRequest() {
        when(request.getNativeRequest(HttpServletRequest.class)).thenReturn(null);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Test detail");
        problemDetail.setTitle("Test Problem");
        problemDetail.setType(URI.create("https://example.com/problem"));
        ResponseEntity<ProblemDetail> entity = ResponseEntity.badRequest().body(problemDetail);

        ResponseEntity<ProblemDetail> result = exceptionTranslator.process(entity, request);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getProperties()).doesNotContainKey("path");
    }

    @Test
    void shouldNotProcessNonDefaultOrConstraintViolationProblem() {
        ProblemDetail customProblemDetail = new ProblemDetail() {
            @Override
            public URI getType() {
                return URI.create("https://example.com/custom");
            }

            @Override
            public String getTitle() {
                return "Custom Problem";
            }

            @Override
            public int getStatus() {
                return 400;
            }

            @Override
            public String getDetail() {
                return null;
            }

            @Override
            public URI getInstance() {
                return null;
            }

            @Override
            public Map<String, Object> getProperties() {
                return Map.of();
            }
        };

        ResponseEntity<ProblemDetail> entity = ResponseEntity.badRequest().body(customProblemDetail);

        ResponseEntity<ProblemDetail> result = exceptionTranslator.process(entity, request);

        assertThat(result).isEqualTo(entity);
    }
}
