package com.crewmeister.cmcodingchallenge.exception;

import com.crewmeister.cmcodingchallenge.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExchangeRateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleExchangeRateNotFound(
            ExchangeRateNotFoundException ex, HttpServletRequest request) {
        log.warn("Exchange rate not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(InvalidCurrencyException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCurrency(
            InvalidCurrencyException ex, HttpServletRequest request) {
        log.warn("Invalid currency: {}", ex.getCurrency());
        return ResponseEntity.badRequest()
            .body(ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Currency")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(BundesBankApiException.class)
    public ResponseEntity<ErrorResponse> handleBundesBankApiError(
            BundesBankApiException ex, HttpServletRequest request) {
        log.error("Bundesbank API error: {}", ex.getMessage());
        HttpStatus status = ex.getStatusCode() >= 500
            ? HttpStatus.SERVICE_UNAVAILABLE
            : HttpStatus.BAD_GATEWAY;

        return ResponseEntity.status(status)
            .body(ErrorResponse.builder()
                .status(status.value())
                .error("External API Error")
                .message("Failed to fetch data from Bundesbank API. Please try again later.")
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(CurrencyLoadException.class)
    public ResponseEntity<ErrorResponse> handleCurrencyLoadError(
            CurrencyLoadException ex, HttpServletRequest request) {
        log.error("Currency load error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
            ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        return ResponseEntity.badRequest()
            .body(ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(message)
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Missing required parameter: " + ex.getParameterName())
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .build());
    }
}
