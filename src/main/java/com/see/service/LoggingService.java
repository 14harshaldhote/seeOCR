package com.see.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoggingService {

    private static final Logger API_LOGGER = LoggerFactory.getLogger(
        "API_LOGGER"
    );
    private static final Logger SECURITY_LOGGER = LoggerFactory.getLogger(
        "SECURITY_LOGGER"
    );
    private static final Logger PERFORMANCE_LOGGER = LoggerFactory.getLogger(
        "PERFORMANCE_LOGGER"
    );

    // MDC Keys for structured logging
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String USER_ID_KEY = "userId";
    private static final String CLIENT_IP_KEY = "clientIP";
    private static final String SESSION_ID_KEY = "sessionId";

    /**
     * Initialize MDC context for request correlation
     */
    public String initializeRequestContext(
        HttpServletRequest request,
        String username
    ) {
        String requestId = generateRequestId();
        String clientIP = getClientIP(request);
        String sessionId = request.getSession(false) != null
            ? request.getSession(false).getId()
            : "no-session";

        MDC.put(REQUEST_ID_KEY, requestId);
        MDC.put(USER_ID_KEY, username != null ? username : "anonymous");
        MDC.put(CLIENT_IP_KEY, clientIP);
        MDC.put(SESSION_ID_KEY, sessionId);

        return requestId;
    }

    /**
     * Clear MDC context
     */
    public void clearRequestContext() {
        MDC.clear();
    }

    /**
     * Generate unique request ID
     */
    private String generateRequestId() {
        return "REQ-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Log API calls with enhanced context
     */
    public void logApiCall(
        HttpServletRequest request,
        String username,
        String action
    ) {
        String clientIP = getClientIP(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");

        // Ensure MDC context is set
        if (MDC.get(REQUEST_ID_KEY) == null) {
            initializeRequestContext(request, username);
        }

        API_LOGGER.info(
            "API_CALL | {} | {} | {} | {} | {} | UA: {} | Ref: {}",
            method,
            uri,
            username != null ? username : "anonymous",
            clientIP,
            action,
            userAgent != null
                ? userAgent.substring(0, Math.min(userAgent.length(), 100))
                : "unknown",
            referer != null ? referer : "direct"
        );
    }

    /**
     * Log user actions with enhanced context
     */
    public void logUserAction(String username, String action, String details) {
        MDC.put(USER_ID_KEY, username != null ? username : "system");
        API_LOGGER.info(
            "USER_ACTION | {} | {} | {}",
            username,
            action,
            details
        );
    }

    /**
     * Log user actions with additional metadata
     */
    public void logUserActionWithMetadata(
        String username,
        String action,
        String details,
        String resource,
        String result
    ) {
        MDC.put(USER_ID_KEY, username != null ? username : "system");
        API_LOGGER.info(
            "USER_ACTION | {} | {} | {} | Resource: {} | Result: {}",
            username,
            action,
            details,
            resource,
            result
        );
    }

    /**
     * Log security events with enhanced tracking
     */
    public void logSecurityEvent(
        String event,
        String username,
        String details
    ) {
        MDC.put(USER_ID_KEY, username != null ? username : "anonymous");
        SECURITY_LOGGER.info(
            "SECURITY_EVENT | {} | {} | {}",
            event,
            username != null ? username : "anonymous",
            details
        );
    }

    /**
     * Log security events with risk level
     */
    public void logSecurityEventWithRisk(
        String event,
        String username,
        String details,
        String riskLevel
    ) {
        MDC.put(USER_ID_KEY, username != null ? username : "anonymous");
        SECURITY_LOGGER.warn(
            "SECURITY_EVENT | {} | {} | {} | Risk: {}",
            event,
            username != null ? username : "anonymous",
            details,
            riskLevel
        );
    }

    /**
     * Log errors with context preservation
     */
    public void logError(String operation, String error, String username) {
        MDC.put(USER_ID_KEY, username != null ? username : "system");
        log.error(
            "ERROR | {} | {} | {}",
            operation,
            username != null ? username : "system",
            error
        );
    }

    /**
     * Log errors with exception details
     */
    public void logError(
        String operation,
        String error,
        String username,
        Throwable exception
    ) {
        MDC.put(USER_ID_KEY, username != null ? username : "system");
        log.error(
            "ERROR | {} | {} | {} | Exception: {}",
            operation,
            username != null ? username : "system",
            error,
            exception != null
                ? exception.getClass().getSimpleName()
                : "unknown",
            exception
        );
    }

    /**
     * Log login attempts with enhanced tracking
     */
    public void logLoginAttempt(
        String username,
        boolean success,
        String clientIP
    ) {
        String status = success ? "SUCCESS" : "FAILED";
        MDC.put(USER_ID_KEY, username);
        MDC.put(CLIENT_IP_KEY, clientIP);

        if (success) {
            SECURITY_LOGGER.info(
                "LOGIN_ATTEMPT | {} | {} | {}",
                username,
                status,
                clientIP
            );
        } else {
            SECURITY_LOGGER.warn(
                "LOGIN_ATTEMPT | {} | {} | {}",
                username,
                status,
                clientIP
            );
        }
    }

    /**
     * Log login attempts with additional context
     */
    public void logLoginAttemptWithContext(
        String username,
        boolean success,
        String clientIP,
        String userAgent,
        String loginMethod
    ) {
        String status = success ? "SUCCESS" : "FAILED";
        MDC.put(USER_ID_KEY, username);
        MDC.put(CLIENT_IP_KEY, clientIP);

        if (success) {
            SECURITY_LOGGER.info(
                "LOGIN_ATTEMPT | {} | {} | {} | Method: {} | UA: {}",
                username,
                status,
                clientIP,
                loginMethod,
                userAgent != null
                    ? userAgent.substring(0, Math.min(userAgent.length(), 50))
                    : "unknown"
            );
        } else {
            SECURITY_LOGGER.warn(
                "LOGIN_ATTEMPT | {} | {} | {} | Method: {} | UA: {}",
                username,
                status,
                clientIP,
                loginMethod,
                userAgent != null
                    ? userAgent.substring(0, Math.min(userAgent.length(), 50))
                    : "unknown"
            );
        }
    }

    /**
     * Log user logout events
     */
    public void logLogout(String username, String clientIP) {
        MDC.put(USER_ID_KEY, username);
        MDC.put(CLIENT_IP_KEY, clientIP);
        SECURITY_LOGGER.info("LOGOUT | {} | {}", username, clientIP);
    }

    /**
     * Log performance metrics
     */
    public void logPerformance(
        String operation,
        long durationMs,
        String username,
        String additionalInfo
    ) {
        MDC.put(USER_ID_KEY, username != null ? username : "system");
        PERFORMANCE_LOGGER.info(
            "PERFORMANCE | {} | {}ms | {} | {}",
            operation,
            durationMs,
            username,
            additionalInfo
        );
    }

    /**
     * Log slow operations
     */
    public void logSlowOperation(
        String operation,
        long durationMs,
        String username,
        String details
    ) {
        MDC.put(USER_ID_KEY, username != null ? username : "system");
        PERFORMANCE_LOGGER.warn(
            "SLOW_OPERATION | {} | {}ms | {} | {}",
            operation,
            durationMs,
            username,
            details
        );
    }

    /**
     * Log data access patterns
     */
    public void logDataAccess(
        String username,
        String operation,
        String entity,
        String entityId,
        String result
    ) {
        MDC.put(USER_ID_KEY, username != null ? username : "system");
        API_LOGGER.info(
            "DATA_ACCESS | {} | {} | {} | ID: {} | Result: {}",
            username,
            operation,
            entity,
            entityId,
            result
        );
    }

    /**
     * Log configuration changes
     */
    public void logConfigChange(
        String username,
        String setting,
        String oldValue,
        String newValue
    ) {
        MDC.put(USER_ID_KEY, username);
        SECURITY_LOGGER.info(
            "CONFIG_CHANGE | {} | Setting: {} | Old: {} | New: {}",
            username,
            setting,
            oldValue,
            newValue
        );
    }

    /**
     * Log API rate limiting events
     */
    public void logRateLimit(
        String clientIP,
        String username,
        String endpoint,
        String action
    ) {
        MDC.put(USER_ID_KEY, username != null ? username : "anonymous");
        MDC.put(CLIENT_IP_KEY, clientIP);
        SECURITY_LOGGER.warn(
            "RATE_LIMIT | {} | {} | {} | {}",
            clientIP,
            username,
            endpoint,
            action
        );
    }

    /**
     * Enhanced client IP detection with multiple header support
     */
    private String getClientIP(HttpServletRequest request) {
        // Check multiple headers in order of preference
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "X-Client-IP",
            "X-Cluster-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (
                ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)
            ) {
                // Take the first IP if there are multiple
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                // Validate IP format (basic check)
                if (isValidIP(ip)) {
                    return ip;
                }
            }
        }

        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    /**
     * Basic IP validation
     */
    private boolean isValidIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // Basic IPv4 validation
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            try {
                for (String part : parts) {
                    int num = Integer.parseInt(part);
                    if (num < 0 || num > 255) {
                        return false;
                    }
                }
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // For IPv6 or other formats, accept them for now
        return ip.length() <= 45; // Max IPv6 length
    }
}
