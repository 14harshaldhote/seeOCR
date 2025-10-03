package com.see.config;

import com.see.service.LoggingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiLoggingFilter extends OncePerRequestFilter {

    private final LoggingService loggingService;

    // Performance thresholds
    private static final long SLOW_REQUEST_THRESHOLD_MS = 2000; // 2 seconds
    private static final long VERY_SLOW_REQUEST_THRESHOLD_MS = 5000; // 5 seconds

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        long startTime = System.currentTimeMillis();
        String requestId = null;

        // Skip logging for static resources and health checks
        if (!shouldLog(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get authentication info
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;

        try {
            // Initialize request context for correlation
            requestId = loggingService.initializeRequestContext(
                request,
                username
            );

            log.debug(
                "Starting request processing: {} {} by user: {}",
                method,
                uri,
                username != null ? username : "anonymous"
            );

            // Process the request
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // Log any exceptions that occur during request processing
            log.error(
                "Exception occurred during request processing: {} {} by user: {}",
                method,
                uri,
                username,
                e
            );

            loggingService.logError(
                "REQUEST_PROCESSING_ERROR",
                "Exception in filter chain: " + e.getMessage(),
                username,
                e
            );
            throw e;
        } finally {
            try {
                // Calculate request duration
                long duration = System.currentTimeMillis() - startTime;
                int statusCode = response.getStatus();

                // Create action description with detailed info
                String action = String.format(
                    "Response: %d, Duration: %dms, Size: %d bytes",
                    statusCode,
                    duration,
                    response.getBufferSize()
                );

                // Log the API call
                loggingService.logApiCall(request, username, action);

                // Log performance metrics for all requests
                loggingService.logPerformance(
                    method + " " + uri,
                    duration,
                    username,
                    "Status: " + statusCode
                );

                // Log slow requests with warning level
                if (duration >= VERY_SLOW_REQUEST_THRESHOLD_MS) {
                    loggingService.logSlowOperation(
                        method + " " + uri,
                        duration,
                        username,
                        String.format(
                            "Very slow request - Status: %d, User: %s",
                            statusCode,
                            username != null ? username : "anonymous"
                        )
                    );
                } else if (duration >= SLOW_REQUEST_THRESHOLD_MS) {
                    loggingService.logSlowOperation(
                        method + " " + uri,
                        duration,
                        username,
                        String.format(
                            "Slow request - Status: %d, User: %s",
                            statusCode,
                            username != null ? username : "anonymous"
                        )
                    );
                }

                // Log security events for certain endpoints
                logSecurityEvents(request, response, username, duration);

                log.debug(
                    "Completed request processing: {} {} by user: {} in {}ms with status: {}",
                    method,
                    uri,
                    username != null ? username : "anonymous",
                    duration,
                    statusCode
                );
            } catch (Exception e) {
                log.error("Error in ApiLoggingFilter finally block", e);
            } finally {
                // Always clear the request context
                loggingService.clearRequestContext();
            }
        }
    }

    /**
     * Log security-relevant events based on request patterns
     */
    private void logSecurityEvents(
        HttpServletRequest request,
        HttpServletResponse response,
        String username,
        long duration
    ) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        int status = response.getStatus();

        // Log authentication related endpoints
        if (uri.contains("/login") || uri.contains("/auth")) {
            if (status >= 200 && status < 300) {
                loggingService.logSecurityEvent(
                    "AUTH_SUCCESS",
                    username,
                    String.format("%s %s completed successfully", method, uri)
                );
            } else if (status == 401 || status == 403) {
                loggingService.logSecurityEventWithRisk(
                    "AUTH_FAILURE",
                    username,
                    String.format(
                        "%s %s failed with status %d",
                        method,
                        uri,
                        status
                    ),
                    "MEDIUM"
                );
            }
        }

        // Log admin operations
        if (uri.startsWith("/api/admin/") || uri.contains("/admin")) {
            loggingService.logSecurityEvent(
                "ADMIN_OPERATION",
                username,
                String.format(
                    "Admin operation: %s %s - Status: %d",
                    method,
                    uri,
                    status
                )
            );
        }

        // Log potential security issues
        if (status == 403) {
            loggingService.logSecurityEventWithRisk(
                "ACCESS_DENIED",
                username,
                String.format("Access denied for %s %s", method, uri),
                "HIGH"
            );
        } else if (status >= 500) {
            loggingService.logSecurityEvent(
                "SERVER_ERROR",
                username,
                String.format(
                    "Server error for %s %s - Status: %d",
                    method,
                    uri,
                    status
                )
            );
        }
    }

    /**
     * Determine if the request should be logged
     */
    private boolean shouldLog(String uri) {
        // Skip static resources
        if (
            uri.startsWith("/static/") ||
            uri.startsWith("/css/") ||
            uri.startsWith("/js/") ||
            uri.startsWith("/images/") ||
            uri.startsWith("/favicon.ico") ||
            uri.startsWith("/webjars/")
        ) {
            return false;
        }

        // Skip health check endpoints
        if (
            uri.equals("/health") ||
            uri.equals("/actuator/health") ||
            uri.equals("/ping")
        ) {
            return false;
        }

        // Log all API calls
        if (uri.startsWith("/api/")) {
            return true;
        }

        // Log important web pages
        if (
            uri.equals("/login") ||
            uri.equals("/logout") ||
            uri.startsWith("/dashboard") ||
            uri.startsWith("/users") ||
            uri.startsWith("/roles") ||
            uri.equals("/")
        ) {
            return true;
        }

        // Log error pages
        if (uri.equals("/error")) {
            return true;
        }

        return false;
    }
}
