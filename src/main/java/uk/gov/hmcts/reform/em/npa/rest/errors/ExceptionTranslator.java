package uk.gov.hmcts.reform.em.npa.rest.errors;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 * The error response follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807)
 */
@ControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

    /**
     * Post-process the Problem payload to add the message key for the front-end if needed
     */
//    @Override
//    public ResponseEntity<Problem> process(@Nullable ResponseEntity<Problem> entity, NativeWebRequest request) {
//        if (entity == null) {
//            return entity;
//        }
//        Problem problem = entity.getBody();
//        if (!(problem instanceof ConstraintViolationProblem || problem instanceof DefaultProblem)) {
//            return entity;
//        }
//        ProblemBuilder builder = Problem.builder()
//            .withType(Problem.DEFAULT_TYPE.equals(problem.getType()) ? ErrorConstants.DEFAULT_TYPE : problem.getType())
//            .withStatus(problem.getStatus())
//            .withTitle(problem.getTitle());
//
//        if (Objects.nonNull(request)) {
//            HttpServletRequest httpServletRequest = request.getNativeRequest(HttpServletRequest.class);
//            if (Objects.nonNull(httpServletRequest)) {
//                builder.with("path", httpServletRequest.getRequestURI());
//            }
//        }
//
//        if (problem instanceof ConstraintViolationProblem) {
//            builder
//                .with("violations", ((ConstraintViolationProblem) problem).getViolations())
//                .with("detail", ErrorConstants.ERR_VALIDATION);
//        } else {
//            builder
//                .withCause(((DefaultProblem) problem).getCause())
//                .withDetail(problem.getDetail())
//                .withInstance(problem.getInstance());
//            problem.getParameters().forEach(builder::with);
//            if (!problem.getParameters().containsKey("detail") && problem.getStatus() != null) {
//                builder.with("detail", "error.http." + problem.getStatus().getStatusCode());
//            }
//        }
//        return new ResponseEntity<>(builder.build(), entity.getHeaders(), entity.getStatusCode());
//    }

    @ExceptionHandler
    public ResponseEntity<Object> handle(Exception ex, NativeWebRequest request) throws Exception {
        if (AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class) != null) {
            // Rethrow Exception
            throw ex;
        }
        if (ex instanceof ErrorResponse) {
            return handleExceptionInternal(ex, null, new HttpHeaders(),
                    HttpStatus.INTERNAL_SERVER_ERROR, request);
        }
        return createResponseEntity(
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "error.http.500"),
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        BindingResult result = ex.getBindingResult();
        List<FieldErrorVM> fieldErrors = result.getFieldErrors().stream()
                .map(f -> new FieldErrorVM(f.getObjectName(), f.getField(), f.getCode()))
                .collect(Collectors.toList());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ErrorConstants.ERR_VALIDATION);
        problemDetail.setType(ErrorConstants.CONSTRAINT_VIOLATION_TYPE);
        problemDetail.setTitle("Method argument not valid");
        problemDetail.setProperty("fieldErrors", fieldErrors);

        return createResponseEntity(problemDetail, headers, status, request);
    }

    @ExceptionHandler(CustomParameterizedException.class)
    public ProblemDetail handleCustomParameterizedException(CustomParameterizedException ex, NativeWebRequest request) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler
    public ProblemDetail handleNoSuchElementException(NoSuchElementException ex, NativeWebRequest request) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ErrorConstants.ENTITY_NOT_FOUND_TYPE);
    }

    @ExceptionHandler
    public ProblemDetail handleBadRequestAlertException(BadRequestAlertException ex, NativeWebRequest request) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ErrorConstants.BAD_REQUEST);
    }

    @ExceptionHandler
    public ProblemDetail handleConcurrencyFailure(ConcurrencyFailureException ex, NativeWebRequest request) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ErrorConstants.ERR_CONCURRENCY_FAILURE);
    }

    @ExceptionHandler
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, NativeWebRequest request) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ErrorConstants.ERR_FORBIDDEN);
    }

    @ExceptionHandler
    public ProblemDetail handleUnAuthorised(BadCredentialsException ex, NativeWebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ErrorConstants.ERR_UNAUTHORISED);
        problemDetail.setProperty("path", request.getDescription(false).substring(4));

        return problemDetail;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletResponse response = servletWebRequest.getResponse();
            if (response != null && response.isCommitted()) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Response already committed. Ignoring: " + ex);
                }
                return null;
            }
        }

        if (statusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        }

        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.updateAndGetBody(this.getMessageSource(), LocaleContextHolder.getLocale());
            ProblemDetail problemDetail = (ProblemDetail) body;
            if (problemDetail.getType().equals(URI.create("about:blank"))) {
                problemDetail.setDetail("error.http." + statusCode.value());
                problemDetail.setProperty("path", request.getDescription(false));

            }
        }

        return createResponseEntity(body, headers, statusCode, request);
    }
}
