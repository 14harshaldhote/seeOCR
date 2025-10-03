package com.see.controllers;


import com.see.dto.RoleDto;
import com.see.service.AdminRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.authorization.AuthorityReactiveAuthorizationManager.hasRole;

@Slf4j
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @PostMapping
    public ResponseEntity<?> createRole(@Valid @RequestBody RoleDto roleDto){
        try{
            log.info("Creating new role: {}", roleDto.getName());
            RoleDto createdRole = adminRoleService.createRole(roleDto);
            return ResponseEntity.ok("Role created successfully");
        } catch (RuntimeException e){
            log.error("Error creating role: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error creating role");
        } catch (Exception e){
            log.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.status(500).body("Unexpected error occurred");
        }
    }

    @GetMapping
    public ResponseEntity<List<RoleDto>> getAllRoles(){
        try{
            log.info("Admin fetching all roles");
            List<RoleDto> roles = adminRoleService.getAllRoles();
            return ResponseEntity.ok(roles);
        }
        catch (RuntimeException e){
            log.error("Error fetching roles: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        } catch (Exception e){
            log.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoleById(@PathVariable UUID id){
        try {
            log.info("Admin fetching role by ID: {}", id);
            return adminRoleService.getRoleById(id)
                    .map(role -> ResponseEntity.ok().body(role))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e){
            log.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.status(500).body("Unexpected error occurred");
        }
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<?> getRoleByName(@PathVariable String name){
        try {
            log.info("Admin fetching role by name: {}", name);
            return adminRoleService.getRoleByName(name)
                    .map(role -> ResponseEntity.ok().body(role))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e){
            log.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.status(500).body("Unexpected error occurred");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRole(@PathVariable UUID id, @Valid @RequestBody RoleDto roleDto){
        try {
            log.info("Admin updating role ID: {}", id);
            RoleDto updatedRole = adminRoleService.updateRole(id, roleDto);
            return ResponseEntity.ok("Role updated successfully");
        } catch (RuntimeException e){
            log.error("Error updating role: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error updating role");
        } catch (Exception e){
            log.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.status(500).body("Unexpected error occurred");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable UUID id) {
        try {
            log.info("Admin deleting role with ID: {}", id);
            adminRoleService.deleteRole(id);
            return ResponseEntity.ok().body("Role deleted successfully");
        } catch (RuntimeException e) {
            log.error("Error deleting role: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error deleting role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred");
        }
    }
}