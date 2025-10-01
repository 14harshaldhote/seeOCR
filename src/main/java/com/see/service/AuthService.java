package com.see.service;

import com.see.config.JwtUtil;
import com.see.dto.AuthResponse;
import com.see.dto.LoginRequest;
import com.see.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserService userService;

    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.getUsername());

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
        User user = userService.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Generate JWT token
        String jwt = jwtUtil.generateToken(userDetails);

        // Get user roles
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        log.info("User {} authenticated successfully", loginRequest.getUsername());

        // Build response
        return AuthResponse.builder()
                .token(jwt)
                .username(userDetails.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    public String refreshToken(String token) {
        String jwt = token.substring(7); // Remove "Bearer " prefix
        String username = jwtUtil.extractUsername(jwt);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (jwtUtil.validateToken(jwt, userDetails)) {
            return jwtUtil.generateToken(userDetails);
        } else {
            throw new BadCredentialsException("Invalid or expired token");
        }
    }
}
