//package com.see.config;
//
//import com.see.domain.Role;
//import com.see.domain.User;
//import com.see.service.RoleService;
//import com.see.service.UserService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.util.HashSet;
//import java.util.Set;
//
//@Component
//@RequiredArgsConstructor
//public class DataLoader implements CommandLineRunner {
//
//    private final UserService userService;
//    private final RoleService roleService;
//    private final PasswordEncoder passwordEncoder;
//
//    @Value("${app.admin.username}")
//    private String adminUsername;
//
//    @Value("${app.admin.email}")
//    private String adminEmail;
//
//    @Value("${app.admin.password}")
//    private String adminPassword;
//
//    @Override
//    public void run(String... args) throws Exception {
//        System.out.println("🚀 DataLoader starting - Initializing seed data...");
//        try {
//            loadRoles();
//            loadAdminUser();
//            System.out.println("✅ DataLoader completed successfully!");
//        } catch (Exception e) {
//            System.err.println("❌ DataLoader failed: " + e.getMessage());
//            throw e;
//        }
//    }
//
//    private void loadRoles() {
//        System.out.println("📝 Loading roles...");
//
//        // Create ADMIN role
//        if (roleService.findByName("ADMIN").isEmpty()) {
//            Role adminRole = new Role();
//            adminRole.setName("ADMIN");
//            adminRole.setDescription("Administrator role with full access");
//            roleService.save(adminRole);
//            System.out.println("✅ Created ADMIN role");
//        } else {
//            System.out.println("ℹ️ ADMIN role already exists");
//        }
//
//        // Create USER role
//        if (roleService.findByName("USER").isEmpty()) {
//            Role userRole = new Role();
//            userRole.setName("USER");
//            userRole.setDescription("Regular user role");
//            roleService.save(userRole);
//            System.out.println("✅ Created USER role");
//        } else {
//            System.out.println("ℹ️ USER role already exists");
//        }
//    }
//
//    private void loadAdminUser() {
//        System.out.println("👤 Loading admin user...");
//
//        if (userService.findByUsername(adminUsername).isEmpty()) {
//            User admin = new User();
//            admin.setUsername(adminUsername);
//            admin.setEmail(adminEmail);
//            admin.setPassword(passwordEncoder.encode(adminPassword));
//            admin.setActive(true);
//
//            // Assign ADMIN role
//            Set<Role> roles = new HashSet<>();
//            roleService.findByName("ADMIN").ifPresent(roles::add);
//            admin.setRoles(roles);
//
//            userService.save(admin);
//            System.out.println("✅ Created admin user: " + adminUsername + " with email: " + adminEmail);
//        } else {
//            System.out.println("ℹ️ Admin user already exists: " + adminUsername);
//        }
//    }
//}
