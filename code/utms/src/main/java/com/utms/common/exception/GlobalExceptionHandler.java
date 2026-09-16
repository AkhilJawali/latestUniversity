package com.utms.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex, WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleViolationException ex, WebRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        List<Map<String, Object>> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.<String, Object>of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage(),
                        "rejectedValue", String.valueOf(error.getRejectedValue())
                ))
                .toList();
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        // A unique/foreign-key constraint was violated at the database level (e.g. recreating a
        // soft-deleted record whose unique key still occupies the non-partial constraint). Surface a
        // clean 409 rather than leaking internals via the generic 500 branch.
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "The operation conflicts with existing data (a record with the same unique key may already exist).",
                request, null);
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex, WebRequest request) {
        // A4-19 PD-105: a concurrent action raced this one and won; the caller's view is stale.
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "This item was modified by another user. Reload and try again.", request, null);
    }

    // --- A wrong URL, verb, parameter or body is the caller's mistake, not a server fault.
    // Without these handlers, Spring's own signals fall through to the generic branch below
    // and every one of them reads as "500 — An unexpected error occurred", which hides the
    // real cause (an unmapped endpoint is indistinguishable from a crash).

    // Boot 3.3 / Framework 6.1 raises NoResourceFoundException for an unmatched path;
    // NoHandlerFoundException is kept for when throw-exception-if-no-handler-found is set.
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNoHandler(Exception ex, WebRequest request) {
        log.warn("No endpoint matched: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND,
                "No endpoint matches this URL. Check the path (and that the server is running the current build).",
                request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String[] supported = ex.getSupportedMethods();
        String allowed = (supported == null || supported.length == 0)
                ? ""
                : " Allowed: " + String.join(", ", supported) + ".";
        log.warn("Method not supported: {}", ex.getMessage());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED,
                ex.getMethod() + " is not supported on this URL." + allowed, request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(
            MissingServletRequestParameterException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Required parameter '" + ex.getParameterName() + "' is missing.", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        String expected = ex.getRequiredType() == null ? null : ex.getRequiredType().getSimpleName();
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Parameter '" + ex.getName() + "' has an invalid value"
                        + (expected == null ? "." : "; expected a " + expected + "."),
                request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(
            HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Unreadable request body: {}", ex.getMostSpecificCause().getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "The request body is missing or is not valid JSON.", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message,
                                                               WebRequest request, List<Map<String, Object>> details) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        if (details != null && !details.isEmpty()) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
