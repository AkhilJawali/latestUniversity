package com.utms.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * A caller's mistake must not be reported as a server fault. Before these handlers existed,
 * an unmapped URL, a wrong verb and a bad query parameter all fell through to the generic
 * Exception branch and surfaced as "500 — An unexpected error occurred", making a stale
 * deployment indistinguishable from a crash.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GlobalExceptionHandlerTest {

    private static final String PATH = "/api/v1/timetables";

    @Mock
    private WebRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getDescription(false)).thenReturn("uri=" + PATH);
    }

    private static int status(ResponseEntity<Map<String, Object>> response) {
        return (int) response.getBody().get("status");
    }

    private static String message(ResponseEntity<Map<String, Object>> response) {
        return String.valueOf(response.getBody().get("message"));
    }

    @Test
    void unmatchedUrlIsNotFound() {
        ResponseEntity<Map<String, Object>> response = handler.handleNoHandler(
                new NoResourceFoundException(HttpMethod.GET, PATH), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, status(response));
        assertEquals(PATH, response.getBody().get("path"));
    }

    @Test
    void unsupportedVerbIsMethodNotAllowedAndListsWhatIsAllowed() {
        ResponseEntity<Map<String, Object>> response = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("DELETE", List.of("GET", "POST")), request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(405, status(response));
        assertTrue(message(response).contains("DELETE"), message(response));
        assertTrue(message(response).contains("GET, POST"), message(response));
    }

    @Test
    void missingRequiredParameterIsBadRequestAndNamesTheParameter() {
        ResponseEntity<Map<String, Object>> response = handler.handleMissingParam(
                new MissingServletRequestParameterException("departmentId", "Long"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(message(response).contains("departmentId"), message(response));
    }

    @Test
    void unconvertibleParameterIsBadRequestAndNamesTheExpectedType() {
        ResponseEntity<Map<String, Object>> response = handler.handleTypeMismatch(
                new MethodArgumentTypeMismatchException(
                        "NaN", Long.class, "departmentId", null, new IllegalArgumentException("nope")),
                request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(message(response).contains("departmentId"), message(response));
        assertTrue(message(response).contains("Long"), message(response));
    }

    @Test
    void malformedBodyIsBadRequest() {
        ResponseEntity<Map<String, Object>> response = handler.handleUnreadableBody(
                new HttpMessageNotReadableException("Unexpected end of input", null, null), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(message(response).contains("JSON"), message(response));
    }

    @Test
    void genuineFaultIsStillFiveHundredAndLeaksNothing() {
        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(
                new IllegalStateException("connection pool exhausted at com.utms.Secret"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", message(response));
    }
}
