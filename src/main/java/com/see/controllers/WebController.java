package com.see.controllers;

import com.see.dto.RoleDto;
import com.see.dto.UserDto;
import com.see.service.AdminRoleService;
import com.see.service.AdminUserService;
import com.see.service.LoggingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebController {

    private final AdminUserService adminUserService;
    private final AdminRoleService adminRoleService;
    private final LoggingService loggingService;

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        String clientIP = getClientIP(request);
        log.info("Login page accessed from IP: {}", clientIP);
        loggingService.logSecurityEvent(
            "LOGIN_PAGE_ACCESS",
            null,
            "IP: " + clientIP
        );
        return "login";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String dashboard(Model model, HttpServletRequest request) {
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "unknown";

        try {
            log.info("Admin {} accessing dashboard", username);
            loggingService.logUserAction(
                username,
                "DASHBOARD_ACCESS",
                "Accessing admin dashboard"
            );

            List<UserDto> users = adminUserService.getAllUsers();
            List<RoleDto> roles = adminRoleService.getAllRoles();

            model.addAttribute("userCount", users.size());
            model.addAttribute("roleCount", roles.size());
            model.addAttribute("pageTitle", "Dashboard");

            log.debug(
                "Dashboard loaded successfully - Users: {}, Roles: {}",
                users.size(),
                roles.size()
            );
        } catch (Exception e) {
            log.error("Error loading dashboard data for user: {}", username, e);
            loggingService.logError(
                "DASHBOARD_LOAD",
                "Failed to load dashboard: " + e.getMessage(),
                username
            );
            model.addAttribute(
                "error",
                "Error loading dashboard data. Please try again."
            );
        }
        return "dashboard";
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String users(Model model, HttpServletRequest request) {
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "unknown";

        try {
            log.info("Admin {} accessing users management page", username);
            loggingService.logUserAction(
                username,
                "USERS_PAGE_ACCESS",
                "Accessing users management"
            );

            List<UserDto> users = adminUserService.getAllUsers();
            model.addAttribute("users", users);
            model.addAttribute("pageTitle", "Users Management");

            log.debug(
                "Users page loaded successfully with {} users",
                users.size()
            );
        } catch (Exception e) {
            log.error("Error loading users data for admin: {}", username, e);
            loggingService.logError(
                "USERS_PAGE_LOAD",
                "Failed to load users: " + e.getMessage(),
                username
            );
            model.addAttribute(
                "error",
                "Error loading users data. Please refresh the page."
            );
            model.addAttribute("users", List.of());
        }
        return "users";
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public String roles(Model model, HttpServletRequest request) {
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "unknown";

        try {
            log.info("Admin {} accessing roles management page", username);
            loggingService.logUserAction(
                username,
                "ROLES_PAGE_ACCESS",
                "Accessing roles management"
            );

            List<RoleDto> roles = adminRoleService.getAllRoles();
            model.addAttribute("roles", roles);
            model.addAttribute("pageTitle", "Roles Management");

            log.debug(
                "Roles page loaded successfully with {} roles",
                roles.size()
            );
        } catch (Exception e) {
            log.error("Error loading roles data for admin: {}", username, e);
            loggingService.logError(
                "ROLES_PAGE_LOAD",
                "Failed to load roles: " + e.getMessage(),
                username
            );
            model.addAttribute(
                "error",
                "Error loading roles data. Please refresh the page."
            );
            model.addAttribute("roles", List.of());
        }
        return "roles";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "unknown";
        String clientIP = getClientIP(request);

        log.info("User {} logging out from IP: {}", username, clientIP);
        loggingService.logLogout(username, clientIP);

        return "redirect:/login?logout";
    }

    /**
     * Global exception handler for web controller
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(
        Exception e,
        HttpServletRequest request
    ) {
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "anonymous";
        String requestUri = request.getRequestURI();
        String clientIP = getClientIP(request);

        log.error(
            "Unhandled exception in WebController for user: {} on {} from IP: {}",
            username,
            requestUri,
            clientIP,
            e
        );

        loggingService.logError(
            "WEB_CONTROLLER_ERROR",
            "Unhandled exception on " + requestUri + ": " + e.getMessage(),
            username
        );

        ModelAndView mav = new ModelAndView("error");
        mav.addObject(
            "error",
            "An unexpected error occurred. Please try again or contact support."
        );
        mav.addObject("pageTitle", "Error");
        return mav;
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (
            xfHeader == null ||
            xfHeader.isEmpty() ||
            "unknown".equalsIgnoreCase(xfHeader)
        ) {
            String xRealIP = request.getHeader("X-Real-IP");
            if (
                xRealIP != null &&
                !xRealIP.isEmpty() &&
                !"unknown".equalsIgnoreCase(xRealIP)
            ) {
                return xRealIP;
            }
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
