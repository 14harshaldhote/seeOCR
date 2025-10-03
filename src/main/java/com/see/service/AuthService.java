package com.see.service;

import com.see.config.JwtUtil;
import com.see.domain.User;
import com.see.dto.AuthResponse;
import com.see.dto.LoginRequest;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserService userService;
    private final LoggingService loggingService;

    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.getUsername());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(
                loginRequest.getUsername()
            );
            User user = userService
                .findByUsername(loginRequest.getUsername())
                .orElseThrow(() ->
                    new BadCredentialsException("User not found")
                );

            // Generate JWT token
            String jwt = jwtUtil.generateToken(userDetails);

            // Get user roles
            List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

            log.info(
                "User {} authenticated successfully",
                loginRequest.getUsername()
            );

            // Log successful login attempt
            loggingService.logLoginAttempt(
                loginRequest.getUsername(),
                true,
                "API"
            );
            loggingService.logSecurityEvent(
                "API_LOGIN_SUCCESS",
                loginRequest.getUsername(),
                "JWT token generated for API access"
            );

            // Build response
            return AuthResponse.builder()
                .token(jwt)
                .username(userDetails.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
        } catch (AuthenticationException e) {
            log.warn(
                "Authentication failed for user: {}",
                loginRequest.getUsername()
            );

            // Log failed login attempt
            loggingService.logLoginAttempt(
                loginRequest.getUsername(),
                false,
                "API"
            );
            loggingService.logSecurityEvent(
                "API_LOGIN_FAILED",
                loginRequest.getUsername(),
                "Authentication failed: " + e.getMessage()
            );

            throw new BadCredentialsException(
                "Authentication failed: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error(
                "Unexpected error during authentication for user: {}",
                loginRequest.getUsername(),
                e
            );

            // Log error
            loggingService.logError(
                "API_AUTHENTICATION_ERROR",
                "Unexpected error: " + e.getMessage(),
                loginRequest.getUsername()
            );

            throw new BadCredentialsException(
                "Authentication failed due to system error"
            );
        }
    }

    public String refreshToken(String token) {
        try {
            String jwt = token.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(jwt);

            log.info("Token refresh requested for user: {}", username);

            UserDetails userDetails = userDetailsService.loadUserByUsername(
                username
            );

            if (jwtUtil.validateToken(jwt, userDetails)) {
                String newToken = jwtUtil.generateToken(userDetails);

                log.info("Token refreshed successfully for user: {}", username);
                loggingService.logSecurityEvent(
                    "TOKEN_REFRESH_SUCCESS",
                    username,
                    "JWT token refreshed successfully"
                );

                return newToken;
            } else {
                log.warn(
                    "Token refresh failed for user: {} - Invalid token",
                    username
                );
                loggingService.logSecurityEvent(
                    "TOKEN_REFRESH_FAILED",
                    username,
                    "Invalid or expired token"
                );
                throw new BadCredentialsException("Invalid or expired token");
            }
        } catch (Exception e) {
            log.error("Error during token refresh: {}", e.getMessage());
            loggingService.logError(
                "TOKEN_REFRESH_ERROR",
                "Token refresh failed: " + e.getMessage(),
                "system"
            );
            throw new BadCredentialsException(
                "Token refresh failed: " + e.getMessage()
            );
        }
    }
}
