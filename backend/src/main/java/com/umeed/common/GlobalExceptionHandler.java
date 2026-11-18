package com.umeed.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors(ex.getBindingResult()));
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ApiError> handleBinding(BindException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors(ex.getBindingResult()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON", request, List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> handleUploadTooLarge(HttpServletRequest request) {
        return respond(HttpStatusCode.valueOf(413), "Each photo must be 5 MB or smaller", request, List.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        String message = ex.getReason() != null ? ex.getReason() : reasonPhrase(ex.getStatusCode());
        return respond(ex.getStatusCode(), message, request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "You are not allowed to do this", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            return respond(status, reasonPhrase(status), request, List.of());
        }
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.", request, List.of());
    }

    /** A value that could not be read at all (for example a malformed id) gets a plain message, not a technical one. */
    private static List<ApiError.FieldError> fieldErrors(BindingResult result) {
        return result.getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(
                        error.getField(),
                        error.isBindingFailure() ? "Invalid value" : error.getDefaultMessage()))
                .toList();
    }

    /** Messages are written in English; when the app asks for Hindi, known ones are swapped for their Hindi version. */
    private static ResponseEntity<ApiError> respond(
            HttpStatusCode status, String message, HttpServletRequest request, List<ApiError.FieldError> fieldErrors) {
        if (HindiMessages.wanted(request)) {
            message = HindiMessages.translate(message);
            fieldErrors = fieldErrors.stream()
                    .map(error -> new ApiError.FieldError(error.field(), HindiMessages.translate(error.message())))
                    .toList();
        }
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), reasonPhrase(status), message, request.getRequestURI(), fieldErrors));
    }

    private static String reasonPhrase(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.getReasonPhrase() : "Error";
    }
}
