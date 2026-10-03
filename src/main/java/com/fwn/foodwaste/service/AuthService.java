package com.fwn.foodwaste.service;

import com.fwn.foodwaste.dto.Request.LoginRequest;
import com.fwn.foodwaste.dto.Request.RegisterRequest;
import com.fwn.foodwaste.dto.Response.AuthResponse;
import com.fwn.foodwaste.entity.CollectionCentres;
import com.fwn.foodwaste.entity.Role;
import com.fwn.foodwaste.entity.User;
import com.fwn.foodwaste.entity.enums.RoleName;
import com.fwn.foodwaste.exception.ResourceNotFoundException;
import com.fwn.foodwaste.exception.ValidationException;
import com.fwn.foodwaste.repository.CollectionCenterRepository;
import com.fwn.foodwaste.repository.RoleRepository;
import com.fwn.foodwaste.repository.UserRepository;
import com.fwn.foodwaste.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager      authenticationManager;
    private final UserRepository             userRepository;
    private final RoleRepository             roleRepository;
    private final CollectionCenterRepository collectionCenterRepository;
    private final PasswordEncoder            passwordEncoder;
    private final JwtTokenProvider           jwtTokenProvider;

    // ── LOGIN ─────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));

        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();

        return buildAuthResponse(user, token);
    }

    // ── REGISTER ──────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // duplicate checks
        if (userRepository.existsByUsername(request.getUsername()))
            throw new ValidationException(
                    "Username '" + request.getUsername()
                            + "' is already taken");

        if (userRepository.existsByEmail(request.getEmail()))
            throw new ValidationException(
                    "Email '" + request.getEmail()
                            + "' is already registered");

        // resolve roles — default to ROLE_DONOR
        Set<Role> roles;
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            roles = Set.of(fetchRole(RoleName.ROLE_DONOR));
        } else {
            roles = request.getRoles().stream()
                    .map(r -> fetchRole(RoleName.valueOf(r)))
                    .collect(Collectors.toSet());
        }

        boolean isOperator = roles.stream()
                .anyMatch(r -> r.getRole() == RoleName.ROLE_OPERATOR);

        // build user — set name/address/phone directly from request
        // name falls back to username ONLY if not provided
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName() != null
                        && !request.getName().isBlank()
                        ? request.getName()
                        : null)               // ← null if not provided, NOT username
                .address(request.getAddress() != null
                        && !request.getAddress().isBlank()
                        ? request.getAddress()
                        : null)               // ← null if not provided
                .phone(request.getPhone() != null
                        && !request.getPhone().isBlank()
                        ? request.getPhone()
                        : null)               // ← null if not provided
                .active(!isOperator)
                .roles(roles)
                .build();

        // link collection centers if donor provided them
        if (request.getCollectionCenterIds() != null
                && !request.getCollectionCenterIds().isEmpty()) {
            List<CollectionCentres> centers = request
                    .getCollectionCenterIds().stream()
                    .map(id -> collectionCenterRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Collection center not found: " + id)))
                    .collect(Collectors.toList());
            user.setCollectionCentres(centers);
        }

        // ONE save only — no second save needed
        userRepository.save(user);

        // operators need admin approval — return without token
        if (!user.isActive()) {
            return buildAuthResponse(user, null);
        }

        // auto-login for active accounts
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));

        return buildAuthResponse(user,
                jwtTokenProvider.generateToken(authentication));
    }

    // ── HELPERS ───────────────────────────────────────────────────────

    private Role fetchRole(RoleName role) {
        return roleRepository.findByRole(role)
                .orElseThrow(() -> new ValidationException(
                        "Role not found: " + role
                                + ". Make sure DataSeeder ran on startup."));
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getRole().name())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roleNames)
                .build();
    }
}