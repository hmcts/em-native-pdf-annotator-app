package uk.gov.hmcts.reform.em.npa.rest.errors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

    MessageSource messageSource;

    @Autowired
    public ExceptionTranslator(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex,
                                                                            WebRequest request) {
        return handleExceptionInternal(ex, null, new HttpHeaders(), HttpStatusCode.valueOf(400), request);
    }

    @Override
    protected ResponseEntity handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        if (ex.hasFieldErrors()) {
            String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> messageSource.getMessage(fieldError, Locale.UK))
                    .collect(Collectors.joining(" AND "));
            ProblemDetail problemDetail =
                    ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), "Request validation failed");
            problemDetail.setProperty("error", errorMessage);
            return handleExceptionInternal(ex, problemDetail, headers, HttpStatusCode.valueOf(422), request);
        }
        return handleExceptionInternal(ex, null, new HttpHeaders(), HttpStatusCode.valueOf(422), request);
    }


    @Override
    protected ResponseEntity handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        if (Objects.isNull(body)) {
            ProblemDetail problemDetail;
            if (ex instanceof ErrorResponse errorResponse) {
                if (statusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
                    statusCode = errorResponse.getStatusCode();
                }
                problemDetail = ProblemDetail.forStatusAndDetail(statusCode, errorResponse.getDetailMessageCode());
            } else {
                problemDetail = ProblemDetail.forStatusAndDetail(statusCode, ex.getLocalizedMessage());
            }

            String rootCauseMessage = ExceptionUtils.getRootCauseMessage(ex);
            problemDetail.setProperty("error", rootCauseMessage.substring(rootCauseMessage.indexOf(":") + 2));
            body = problemDetail;
        }

        return ResponseEntity
                .status(statusCode)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        // Routes straight into your custom ProblemDetail layout with an HTTP 403 Forbidden status
        return handleExceptionInternal(ex, null, new HttpHeaders(), HttpStatusCode.valueOf(403), request);
    }

    @ExceptionHandler(RuntimeException.class) // 👈 Explicitly target runtime exceptions to unblock MockMvc mapping
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex, WebRequest request) {
        return handleExceptionInternal(ex, null, new HttpHeaders(), HttpStatusCode.valueOf(500), request);
    }
}
