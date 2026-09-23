package com.fwn.foodwaste.controller;

import com.fwn.foodwaste.dto.Response.UserResponse;
import com.fwn.foodwaste.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")   // entire controller is ADMIN only
public class UserController {

    private final UserService userService;

    // GET /api/users
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    // GET /api/users/{id}
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * PUT /api/users/{id}/roles
     * Body: ["ROLE_ADMIN", "ROLE_OPERATOR"]
     * Replaces ALL current roles on the user with the supplied set.
     */
    @PutMapping("/{id}/roles")
    public ResponseEntity<UserResponse> assignRoles(
            @PathVariable Long id,
            @RequestBody Set<String> roles) {
        return ResponseEntity.ok(userService.assignRoles(id, roles));
    }

    /**
     * PATCH /api/users/{id}/status?active=false
     * Activates or deactivates a user account.
     * Deactivated users cannot log in (Spring Security checks enabled flag).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> setStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(userService.setActiveStatus(id, active));
    }

    // DELETE /api/users/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
