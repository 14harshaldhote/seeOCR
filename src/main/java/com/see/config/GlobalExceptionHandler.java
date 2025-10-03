package com.see.config;

import com.see.service.LoggingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final LoggingService loggingService;

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        String username = getCurrentUsername();
        String requestUri = request.getRequestURI();

        log.warn(
            "Validation error on {} by user: {} - {}",
            requestUri,
            username,
            ex.getMessage()
        );

        loggingService.logUserAction(
            username,
            "VALIDATION_ERROR",
            "Validation failed for " + requestUri + ": " + ex.getMessage()
        );

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = createErrorResponse(
            "Validation failed",
            "VALIDATION_ERROR",
            HttpStatus.BAD_REQUEST.value(),
            errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle constraint violation errors
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        String username = getCurrentUsername();
        String requestUri = request.getRequestURI();

        log.warn(
            "Constraint violation on {} by user: {} - {}",
            requestUri,
            username,
            ex.getMessage()
        );

        loggingService.logUserAction(
            username,
            "CONSTRAINT_VIOLATION",
            "Constraint violation for " + requestUri + ": " + ex.getMessage()
        );

        Map<String, Object> response = createErrorResponse(
            "Constraint violation: " + ex.getMessage(),
            "CONSTRAINT_VIOLATION",
            HttpStatus.BAD_REQUEST.value(),
            null
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle authentication errors
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
        BadCredentialsException ex,
        HttpServletRequest request
    ) {
        String username = getCurrentUsername();
        String requestUri = request.getRequestURI();
        String clientIP = getClientIP(request);

        log.warn(
            "Authentication failed on {} from IP {} - {}",
            requestUri,
            clientIP,
            ex.getMessage()
        );

        loggingService.logSecurityEventWithRisk(
            "AUTHENTICATION_FAILED",
            username,
            "Bad credentials for " + requestUri + " from IP: " + clientIP,
            "HIGH"
        );

        Map<String, Object> response = createErrorResponse(
            "Authentication failed",
            "AUTHENTICATION_ERROR",
            HttpStatus.UNAUTHORIZED.value(),
            null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle access denied errors
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
        AccessDeniedException ex,
        HttpServletRequest request
    ) {
        String username = getCurrentUsername();
        String requestUri = request.getRequestURI();
        String clientIP = getClientIP(request);

        log.warn(
            "Access denied for user {} on {} from IP {} - {}",
            username,
            requestUri,
            clientIP,
            ex.getMessage()
        );

        loggingService.logSecurityEventWithRisk(
            "ACCESS_DENIED",
            username,
            "Access denied for " + requestUri + " from IP: " + clientIP,
            "MEDIUM"
        );

        if (isApiRequest(request)) {
            Map<String, Object> response = createErrorResponse(
                "Access denied",
                "ACCESS_DENIED",
                HttpStatus.FORBIDDEN.value(),
                null
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } else {
            // For web requests, redirect to access denied page
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse(
                    "Access denied",
                    "ACCESS_DENIED",
                    HttpStatus.FORBIDDEN.value(),
                    null
                ));
        }
    }

    /**
     * Handle method not supported errors
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
        HttpRequestMethodNotSupportedException ex,
        HttpServletRequest request
    ) {
        String username = getCurrentUsername();
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        log.warn(
            "Method {} not supported for {} by user: {}",
            method,
            requestUri,
            username
        );

        loggingService.logUserAction(
            username,
            "METHOD_NOT_SUPPORTED",
            "Method " + method + " not supported for " + requestUri
        );

        Map<String, Object> response = createErrorResponse(
            "Method " + method + " not supported for this endpoint",
            "METHOD_NOT_ALLOWED",
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            null
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Handle argument type mismatch errors
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
        MethodArgumentTypeMismatchException ex,
        HttpServletRequest request
    ) {
        String username = getCurrentUsername();
        String requestUri = request.getRequestURI();

        log.warn(
            "Type mismatch on {} by user: {} - Parameter: {}, Value: {}, Expected: {}",
            requestUri,
            username,
            ex.getName(),
            ex.getValue(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        loggingService.logUserAction(
            username,
            "TYPE_MISMATCH_ERROR",
            "Type mismatch for parameter " + ex.getName() + " on " + requestUri
        );

        String message = String.format(
            "Invalid value '%s' for parameter '%s'. Expected type: %s",
            ex.getValue(),
            ex.getName(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        Map<String, Object> response = createErrorResponse(
            message,
            "TYPE_MISMATCH",
            HttpStatus.BAD_REQUEST.value(),
            null
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle 404 errors
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, Object>> handleNotFound(
        NoHandlerFoundException ex,
        HttpServletRequest request
    ) {
        String username = getCurrentUsername();
        String requestUri = request.getRequestURI();

        log.warn("404 - Resource not found: {} requested by user: {}", requestUri, username);

        loggingService.logUserAction(
            username,
            "RESOURCE_NOT_FOUND",
            "404 error for " + requestUri
        );

        Map<String, Object> response = createErrorResponse(
            "Resource not found: " + requestUri,
            "NOT_FOUND",
            HttpStatus.NOT_FOUND.value(),
            null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle database errors
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(
        DataAccessException ex,
        HttpServletRequest request
    ) {
        String username = getCurrentUsername();
        String requestUri = request.getRequestURI();

        log.error(
            "Database error on {} by user: {} - {}",
            requestUri,
            username,
            ex.getMessage(),
            ex
        );

        loggingService.logError(
            "DATABASE_ERROR",
            "Database access error for " + requestUri + ": " + ex.getMessage(),
            username,
            ex
        );

        Map<String, Object> response = createErrorResponse(
            "Database error occurred. Please try again later.",
            "DATABASE_ERROR",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle data integrity violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
        DataIntegrityViolationException ex,
        HttpServletRequest request
    ) {
        String username = getCurrentUsername();
        String requestUri = request.getRequestURI();

        log.warn(
            "Data integrity violation on {} by user: {} - {}",
            requestUri,
            username,
            ex.getMessage()
        );

        loggingService.logUserAction(
            username,
            "DATA_INTEGRITY_VIOLATION",
            "Data integrity violation for " + requestUri + ": " + ex.getMessage()
        );

        String message = ex.getMessage();
        if (message != null && message.contains("Duplicate entry")) {
            message = "A record with this information already exists";
        } else {
            message = "Data integrity constraint violated";
        }

        Map<String, Object> response = createErrorResponse(
            message,
            "DATA_INTEGRITY_VIOLATION",
            HttpStatus.CONFLICT.value(),
            null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle custom runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object handleRuntimeException(
        RuntimeException ex,
        HttpServletRequest request
    ) {
        String username = getCurrentUsername();
        String requestUri = request.getRequestURI();

        log.error(
            "Runtime exception on {} by user: {} - {}",
            requestUri,
            username,
            ex.getMessage(),
            ex
        );

        loggingService.logError(
            "RUNTIME_EXCEPTION",
            "Runtime exception for " + requestUri + ": " + ex.getMessage(),
            username,
            ex
        );

        if (isApiRequest(request)) {
            Map<String, Object> response = createErrorResponse(
                "An error occurred: " + ex.getMessage(),
                "RUNTIME_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } else {
            ModelAndView mav = new ModelAndView("error");
            mav.addObject("error", "An unexpected error occurred. Please try again.");
            mav.addObject("pageTitle", "Error");
            return mav;
        }
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {
        String username = getCurrentUsername();
        String requestUri = request.getRequestURI();
        String clientIP = getClientIP(request);

        log.error(
            "Unhandled exception on {} by user: {} from IP: {} - {}",
            requestUri,
            username,
            clientIP,
            ex.getMessage(),
            ex
        );

        loggingService.logError(
            "UNHANDLED_EXCEPTION",
            "Unhandled exception for " + requestUri + ": " + ex.getMessage(),
            username,
            ex
        );

        if (isApiRequest(request)) {
            Map<String, Object> response = createErrorResponse(
                "An internal server error occurred. Please contact support.",
                "INTERNAL_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } else {
            ModelAndView mav = new ModelAndView("error");
            mav.addObject("error", "An internal server error occurred. Please try again or contact support.");
            mav.addObject("pageTitle", "Internal Server Error");
            return mav;
        }
    }

    /**
     * Create standardized error response
     */
    private Map<String, Object> createErrorResponse(
        String message,
        String errorCode,
        int status,
        Object details
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", message);
        response.put("errorCode", errorCode);
        response.put("status", status);
        response.put("timestamp", System.currentTimeMillis());

        if (details != null) {
            response.put("details", details);
        }

        return response;
    }

    /**
     * Check if request is API request
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");

        return requestURI.startsWith("/api/") ||
               (acceptHeader != null && acceptHeader.contains("application/json"));
    }

    /**
     * Get current authenticated username
     */
    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "anonymous";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Get client IP from request
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
