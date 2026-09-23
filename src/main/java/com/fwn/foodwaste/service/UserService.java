package com.fwn.foodwaste.service;

import com.fwn.foodwaste.entity.Role;
import com.fwn.foodwaste.entity.User;
import com.fwn.foodwaste.dto.Response.UserResponse;
import com.fwn.foodwaste.entity.enums.RoleName;
import com.fwn.foodwaste.repository.RoleRepository;
import com.fwn.foodwaste.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;



    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + id)));
    }



    /**
     * Replaces all current roles on the user with the supplied set.
     * Roles must be valid RoleName strings e.g. "ROLE_ADMIN".
     */
    public UserResponse assignRoles(Long id, Set<String> roleNames) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        Set<Role> roles = roleNames.stream()
                .map(name -> roleRepository
                        .findByRole(RoleName.valueOf(name))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Role not found: " + name)))
                .collect(Collectors.toSet());

        user.setRoles(roles);
        return toResponse(userRepository.save(user));
    }



    public UserResponse setActiveStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setActive(active);
        return toResponse(userRepository.save(user));
    }



    public void delete(Long id) {
        if (!userRepository.existsById(id))
            throw new IllegalArgumentException("User not found: " + id);
        userRepository.deleteById(id);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(role -> role.getRole().name()).collect(Collectors.toSet()))
                .active(user.isActive())
                .name(user.getName())
                .address(user.getAddress())
                .phone(user.getPhone())
                .totalDonations(user.getFoodWasteItems().size())
                .collectionCenterLocations(user.getCollectionCentres().stream().map(center -> center.getLocation()).toList())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
