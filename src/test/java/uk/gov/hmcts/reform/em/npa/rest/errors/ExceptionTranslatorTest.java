package uk.gov.hmcts.reform.em.npa.rest.errors;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExceptionTranslatorTest {

    @Mock
    private NativeWebRequest request;

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

        ResponseEntity<Problem> response = exceptionTranslator.handleNoSuchElementException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(Status.NOT_FOUND);
        assertThat(response.getBody().getParameters())
            .containsEntry("message", ErrorConstants.ENTITY_NOT_FOUND_TYPE);
    }

    @Test
    void shouldHandleBadRequestAlertException() {
        BadRequestAlertException exception = new BadRequestAlertException(
            "Bad request",
            "entityName",
            "errorKey"
        );

        ResponseEntity<Problem> response = exceptionTranslator.handleBadRequestAlertException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(Status.BAD_REQUEST);
        assertThat(response.getBody().getParameters())
            .containsEntry("message", ErrorConstants.BAD_REQUEST);
    }

    @Test
    void shouldHandleConcurrencyFailureException() {
        ConcurrencyFailureException exception = new ConcurrencyFailureException("Concurrency failure");

        ResponseEntity<Problem> response = exceptionTranslator.handleConcurrencyFailure(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(Status.CONFLICT);
        assertThat(response.getBody().getParameters())
            .containsEntry("message", ErrorConstants.ERR_CONCURRENCY_FAILURE);
    }

    @Test
    void shouldHandleAccessDeniedException() {
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        ResponseEntity<Problem> response = exceptionTranslator.handleAccessDenied(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(Status.FORBIDDEN);
        assertThat(response.getBody().getParameters())
            .containsEntry("message", ErrorConstants.ERR_FORBIDDEN);
    }

    @Test
    void shouldHandleBadCredentialsException() {
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        ResponseEntity<Problem> response = exceptionTranslator.handleUnAuthorised(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(Status.UNAUTHORIZED);
        assertThat(response.getBody().getParameters())
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

        ResponseEntity<Problem> response = exceptionTranslator.handleMethodArgumentNotValid(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(Status.BAD_REQUEST);
        assertThat(response.getBody().getType()).isEqualTo(ErrorConstants.CONSTRAINT_VIOLATION_TYPE);
        assertThat(response.getBody().getTitle()).isEqualTo("Method argument not valid");
        assertThat(response.getBody().getParameters())
            .containsEntry("message", ErrorConstants.ERR_VALIDATION);
        
        @SuppressWarnings("unchecked")
        List<FieldErrorVM> fieldErrors = (List<FieldErrorVM>) response.getBody().getParameters().get("fieldErrors");
        assertThat(fieldErrors).hasSize(2);
        assertThat(fieldErrors.get(0).getField()).isEqualTo("field1");
        assertThat(fieldErrors.get(0).getMessage()).isEqualTo("NotNull");
        assertThat(fieldErrors.get(1).getField()).isEqualTo("field2");
        assertThat(fieldErrors.get(1).getMessage()).isEqualTo("NotBlank");
    }

    @Test
    void shouldProcessNullEntity() {
        ResponseEntity<Problem> result = exceptionTranslator.process(null, request);

        assertThat(result).isNull();
    }

    @Test
    void shouldProcessConstraintViolationProblem() {
        setupRequestMocks();
        Violation violation = new Violation("field", "must not be null");
        ConstraintViolationProblem problem = new ConstraintViolationProblem(
            Status.BAD_REQUEST,
            List.of(violation)
        );
        ResponseEntity<Problem> entity = ResponseEntity.badRequest().body(problem);

        ResponseEntity<Problem> result = exceptionTranslator.process(entity, request);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getParameters())
            .containsEntry("message", ErrorConstants.ERR_VALIDATION)
            .containsKey("violations")
            .containsKey("path");
        assertThat(result.getBody().getParameters().get("path")).isEqualTo("/test/path");
    }

    @Test
    void shouldProcessDefaultProblem() {
        setupRequestMocks();
        Problem problem = Problem.builder()
            .withType(URI.create("https://example.com/problem"))
            .withTitle("Test Problem")
            .withStatus(Status.INTERNAL_SERVER_ERROR)
            .withDetail("Test detail")
            .with("customParam", "customValue")
            .build();
        ResponseEntity<Problem> entity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);

        ResponseEntity<Problem> result = exceptionTranslator.process(entity, request);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getType()).isEqualTo(URI.create("https://example.com/problem"));
        assertThat(result.getBody().getTitle()).isEqualTo("Test Problem");
        assertThat(result.getBody().getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody().getDetail()).isEqualTo("Test detail");
        assertThat(result.getBody().getParameters())
            .containsEntry("customParam", "customValue")
            .containsEntry("message", "error.http.500")
            .containsEntry("path", "/test/path");
    }

    @Test
    void shouldProcessDefaultProblemWithDefaultType() {
        setupRequestMocks();
        Problem problem = Problem.builder()
            .withType(Problem.DEFAULT_TYPE)
            .withTitle("Default Problem")
            .withStatus(Status.BAD_REQUEST)
            .build();
        ResponseEntity<Problem> entity = ResponseEntity.badRequest().body(problem);

        ResponseEntity<Problem> result = exceptionTranslator.process(entity, request);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getType()).isEqualTo(ErrorConstants.DEFAULT_TYPE);
    }

    @Test
    void shouldProcessDefaultProblemWithExistingMessageParameter() {
        setupRequestMocks();
        Problem problem = Problem.builder()
            .withType(URI.create("https://example.com/problem"))
            .withTitle("Test Problem")
            .withStatus(Status.BAD_REQUEST)
            .with("message", "custom.error.message")
            .build();
        ResponseEntity<Problem> entity = ResponseEntity.badRequest().body(problem);

        ResponseEntity<Problem> result = exceptionTranslator.process(entity, request);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getParameters())
            .containsEntry("message", "custom.error.message");
    }

    @Test
    void shouldProcessProblemWithNullRequest() {
        Problem problem = Problem.builder()
            .withType(URI.create("https://example.com/problem"))
            .withTitle("Test Problem")
            .withStatus(Status.BAD_REQUEST)
            .build();
        ResponseEntity<Problem> entity = ResponseEntity.badRequest().body(problem);

        ResponseEntity<Problem> result = exceptionTranslator.process(entity, null);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getParameters()).doesNotContainKey("path");
    }

    @Test
    void shouldProcessProblemWithNullHttpServletRequest() {
        when(request.getNativeRequest(HttpServletRequest.class)).thenReturn(null);
        
        Problem problem = Problem.builder()
            .withType(URI.create("https://example.com/problem"))
            .withTitle("Test Problem")
            .withStatus(Status.BAD_REQUEST)
            .build();
        ResponseEntity<Problem> entity = ResponseEntity.badRequest().body(problem);

        ResponseEntity<Problem> result = exceptionTranslator.process(entity, request);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getParameters()).doesNotContainKey("path");
    }

    @Test
    void shouldNotProcessNonDefaultOrConstraintViolationProblem() {
        Problem customProblem = new Problem() {
            @Override
            public URI getType() {
                return URI.create("https://example.com/custom");
            }

            @Override
            public String getTitle() {
                return "Custom Problem";
            }

            @Override
            public Status getStatus() {
                return Status.BAD_REQUEST;
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
            public Map<String, Object> getParameters() {
                return Map.of();
            }
        };
        
        ResponseEntity<Problem> entity = ResponseEntity.badRequest().body(customProblem);

        ResponseEntity<Problem> result = exceptionTranslator.process(entity, request);

        assertThat(result).isEqualTo(entity);
    }
}
