package com.see.controllers;

import com.see.dto.AssignRoleRequest;
import com.see.dto.CreateUserRequest;
import com.see.dto.UserDto;
import com.see.service.AdminUserService;
import com.see.service.LoggingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final LoggingService loggingService;

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String adminUser = auth != null ? auth.getName() : "unknown";

            loggingService.logUserAction(adminUser, "CREATE_USER", "Creating user: " + request.getUsername());
            UserDto createdUser = adminUserService.createUser(request);

            loggingService.logUserAction(adminUser, "CREATE_USER_SUCCESS", "User created: " + request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (RuntimeException e) {
            loggingService.logError("CREATE_USER", e.getMessage(), request.getUsername());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }


    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        try {
            log.info("Admin fetching all users");
            List<UserDto> users = adminUserService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error fetching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        try {
            log.info("Admin fetching user by ID: {}", id);
            return adminUserService.getUserById(id)
                    .map(user -> ResponseEntity.ok().body(user))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching user by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            log.info("Admin fetching user by username: {}", username);
            return adminUserService.findByUsername(username)
                    .map(user -> ResponseEntity.ok().body(user))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching user by username: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @Valid @RequestBody UserDto userDto) {
        try {
            log.info("Admin updating user with ID: {}", id);
            UserDto updatedUser = adminUserService.updateUser(id, userDto);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("Error updating user: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }

    @PostMapping("/assign-role")
    public ResponseEntity<?> assignRoleToUser(@Valid @RequestBody AssignRoleRequest request) {
        try {
            log.info("Admin assigning role {} to user {}", request.getRoleId(), request.getUserId());
            UserDto updatedUser = adminUserService.assignRoleToUser(request);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("Error assigning role: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error assigning role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }

    @PostMapping("/remove-role")
    public ResponseEntity<?> removeRoleFromUser(@Valid @RequestBody AssignRoleRequest request) {
        try {
            log.info("Admin removing role {} from user {}", request.getRoleId(), request.getUserId());
            UserDto updatedUser = adminUserService.removeRoleFromUser(request);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("Error removing role: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error removing role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        try {
            log.info("Admin deleting user with ID: {}", id);
            adminUserService.deleteUser(id);
            return ResponseEntity.ok().body("User deleted successfully");
        } catch (RuntimeException e) {
            log.error("Error deleting user: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }
}
