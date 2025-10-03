# SeeOCR Logging System Documentation

## Overview

The SeeOCR application implements a comprehensive logging system designed for identification, tracking, security monitoring, and performance analysis. The system uses SLF4J with Logback as the underlying implementation and provides structured logging with request correlation.

## Log Files Structure

### Location
All log files are stored in the `logs/` directory in the application root.

### Log Files

| File | Purpose | Retention | Max Size |
|------|---------|-----------|----------|
| `application.log` | General application logs | 30 days | 100MB per file |
| `api-calls.log` | API endpoint calls and responses | 30 days | 50MB per file |
| `security.log` | Security events and authentication | 90 days | 50MB per file |
| `errors.log` | Error and warning messages | 90 days | 50MB per file |
| `performance.log` | Performance metrics and slow operations | 7 days | 50MB per file |

## Features

### 1. Request Correlation
- Each request gets a unique Request ID (REQ-xxxxxxxx)
- Request ID is propagated through all log entries for that request
- MDC (Mapped Diagnostic Context) is used for context preservation

### 2. Structured Logging
- Consistent log patterns across all log files
- Contextual information (user, IP, session)
- Machine-readable format for log analysis

### 3. Security Event Tracking
- Login/logout attempts
- Authentication failures
- Access denied events
- Admin operations
- Suspicious activities

### 4. Performance Monitoring
- Request duration tracking
- Slow operation detection (>2s warning, >5s critical)
- Database query performance
- API response times

### 5. Error Handling
- Global exception handling with detailed logging
- Stack trace preservation
- User-friendly error responses
- Error correlation with request context

## Configuration

### Logback Configuration
The logging configuration is defined in `src/main/resources/logback-spring.xml`:

```xml
<!-- Key features -->
- Rolling file policies with size and time-based rotation
- Async appenders for better performance
- Color-coded console output
- Structured patterns with request correlation
- Framework logging level optimization
```

### Application Properties
Key logging settings in `application.properties`:

```properties
# Logging levels
logging.level.com.see=INFO
logging.level.com.see.config=DEBUG
logging.level.com.see.service=INFO
logging.level.com.see.controllers=INFO

# Database query logging
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type=TRACE

# Security logging
logging.level.org.springframework.security=INFO
```

## Log Patterns

### Application Log Pattern
```
yyyy-MM-dd HH:mm:ss.SSS [thread] LEVEL [requestId] logger - message
```

### API Log Pattern
```
yyyy-MM-dd HH:mm:ss.SSS [requestId] [userId] - message
```

### Security Log Pattern
```
yyyy-MM-dd HH:mm:ss.SSS [requestId] [clientIP] - message
```

## Usage Examples

### In Controllers
```java
@RestController
public class MyController {
    
    @GetMapping("/api/data")
    public ResponseEntity<?> getData() {
        log.info("Processing data request for user: {}", getCurrentUser());
        
        // Business logic here
        
        log.debug("Data retrieved successfully, count: {}", data.size());
        return ResponseEntity.ok(data);
    }
}
```

### Security Events
```java
// Login attempt logging
loggingService.logLoginAttempt(username, true, clientIP);

// Security event logging
loggingService.logSecurityEvent("ACCESS_GRANTED", username, "Admin panel access");

// Error logging
loggingService.logError("DATA_VALIDATION", "Invalid user data", username, exception);
```

### Performance Tracking
```java
long startTime = System.currentTimeMillis();
// ... operation ...
long duration = System.currentTimeMillis() - startTime;

loggingService.logPerformance("USER_SEARCH", duration, username, "Found " + results.size() + " users");
```

## Log Analysis

### Finding Issues
```bash
# Find all errors for a specific user
grep "john.doe" logs/errors.log

# Find slow operations
grep "SLOW_OPERATION" logs/performance.log

# Find failed login attempts
grep "LOGIN_ATTEMPT.*FAILED" logs/security.log

# Track a specific request
grep "REQ-12345678" logs/application.log
```

### Security Monitoring
```bash
# Monitor authentication failures
tail -f logs/security.log | grep "AUTH_FAILURE"

# Watch for access denied events
tail -f logs/security.log | grep "ACCESS_DENIED"

# Monitor admin operations
tail -f logs/security.log | grep "ADMIN_OPERATION"
```

## Request Flow Tracking

### Example Log Sequence
```
2024-01-15 10:30:15.123 [http-nio-8080-exec-1] INFO  [REQ-a1b2c3d4] c.s.c.ApiLoggingFilter - Starting request processing: POST /api/admin/users by user: admin
2024-01-15 10:30:15.125 [http-nio-8080-exec-1] INFO  [REQ-a1b2c3d4] [admin] - USER_ACTION | admin | CREATE_USER | Creating user: newuser
2024-01-15 10:30:15.234 [http-nio-8080-exec-1] DEBUG [REQ-a1b2c3d4] c.s.s.AdminUserService - Creating new user: newuser
2024-01-15 10:30:15.345 [http-nio-8080-exec-1] INFO  [REQ-a1b2c3d4] [admin] - USER_ACTION | admin | CREATE_USER_SUCCESS | User created: newuser
2024-01-15 10:30:15.347 [http-nio-8080-exec-1] INFO  [REQ-a1b2c3d4] c.s.c.ApiLoggingFilter - API_CALL | POST | /api/admin/users | admin | 192.168.1.100 | Response: 201, Duration: 224ms
```

## Error Handling

### Exception Logging
All exceptions are automatically logged with:
- Request context (URL, method, user, IP)
- Stack traces
- Request correlation ID
- Categorized error codes

### Error Categories
- `VALIDATION_ERROR`: Input validation failures
- `AUTHENTICATION_ERROR`: Authentication issues
- `ACCESS_DENIED`: Authorization failures
- `DATABASE_ERROR`: Database connectivity/query issues
- `RUNTIME_ERROR`: Application logic errors
- `INTERNAL_ERROR`: Unexpected system errors

## Performance Monitoring

### Metrics Tracked
- Request duration
- Database query time
- API response times
- Slow operations (>2s)
- Very slow operations (>5s)

### Performance Log Example
```
2024-01-15 10:30:15.567 [REQ-a1b2c3d4] - PERFORMANCE | GET /api/users | 1234ms | admin | Status: 200
2024-01-15 10:30:15.568 [REQ-a1b2c3d4] - SLOW_OPERATION | GET /api/users | 1234ms | admin | Slow request - Status: 200, User: admin
```

## Security Features

### Authentication Tracking
- Login attempts (success/failure)
- JWT token generation
- Token refresh events
- Logout events

### Access Control
- Permission denied events
- Admin operation tracking
- Suspicious activity detection
- Rate limiting events

### Audit Trail
- User actions with timestamps
- Data modifications
- Configuration changes
- Administrative operations

## Troubleshooting

### Common Issues

#### 1. 500 Internal Server Errors
```bash
# Check error logs
tail -50 logs/errors.log

# Look for specific error patterns
grep "INTERNAL_ERROR\|RUNTIME_EXCEPTION" logs/errors.log
```

#### 2. Authentication Problems
```bash
# Check security logs
grep "AUTH" logs/security.log | tail -20

# Check failed login attempts
grep "LOGIN_ATTEMPT.*FAILED" logs/security.log
```

#### 3. Performance Issues
```bash
# Find slow operations
grep "SLOW_OPERATION" logs/performance.log | tail -20

# Check database performance
grep "hibernate.SQL" logs/application.log
```

#### 4. Database Connection Issues
```bash
# Check for database errors
grep "DATABASE_ERROR\|HikariCP\|Connection" logs/errors.log
```

### Log Rotation Issues
If logs are not rotating properly:
1. Check disk space: `df -h`
2. Verify log directory permissions: `ls -la logs/`
3. Check Logback configuration syntax
4. Restart the application

### Missing Logs
If logs are missing:
1. Verify `logs/` directory exists and is writable
2. Check Logback configuration path
3. Ensure proper logging levels are set
4. Check for file system issues

## Best Practices

### 1. Log Levels
- **ERROR**: System errors, exceptions that need immediate attention
- **WARN**: Warning conditions, recoverable errors
- **INFO**: Important business events, user actions
- **DEBUG**: Detailed diagnostic information
- **TRACE**: Very detailed diagnostic information

### 2. Sensitive Data
- Never log passwords or secrets
- Mask or truncate sensitive information
- Use parameterized logging to avoid injection

### 3. Performance
- Use parameterized logging: `log.info("User {} logged in", username)`
- Avoid expensive operations in log statements
- Use appropriate log levels for production

### 4. Context Information
- Always include relevant context (user, request ID, operation)
- Use structured logging for machine processing
- Include timing information for performance analysis

## Monitoring and Alerting

### Log Monitoring Tools
Consider integrating with:
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Splunk
- Grafana + Loki
- AWS CloudWatch
- Azure Monitor

### Alerting Scenarios
Set up alerts for:
- High error rates
- Failed authentication attempts
- Slow response times
- Database connection failures
- Disk space issues

## Maintenance

### Regular Tasks
1. **Daily**: Monitor error logs for new issues
2. **Weekly**: Review performance logs for slow operations
3. **Monthly**: Analyze security logs for patterns
4. **Quarterly**: Review and optimize logging configuration

### Cleanup
- Logs are automatically rotated based on time and size
- Old log files are compressed and eventually deleted
- Monitor disk space usage regularly

## Development Guidelines

### Adding New Log Statements
```java
// Good examples
log.info("Processing order {} for customer {}", orderId, customerId);
log.warn("Database connection pool is {}% full", poolUsage);
log.error("Failed to process payment for order {}", orderId, exception);

// Avoid
log.info("Processing order " + orderId + " for customer " + customerId); // String concatenation
log.debug("User details: " + user.toString()); // Expensive operation
```

### Custom Log Categories
To add new log categories:
1. Define logger in LoggingService
2. Add appender in logback-spring.xml
3. Configure rotation and retention policies
4. Update documentation

## Support

For logging system issues:
1. Check this documentation
2. Review application logs for errors
3. Verify configuration settings
4. Contact the development team with specific log excerpts and error messages

---

**Last Updated**: January 2024
**Version**: 1.0
**Maintainer**: SeeOCR Development Team