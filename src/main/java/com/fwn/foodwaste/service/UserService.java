package com.fwn.foodwaste.service;

import com.fwn.foodwaste.entity.User;
import com.fwn.foodwaste.dto.Request.UserDetailsUpdateRequest;
import com.fwn.foodwaste.dto.Response.UserResponse;
import com.fwn.foodwaste.exception.ValidationException;
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



    public UserResponse setActiveStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setActive(active);
        return toResponse(userRepository.save(user));
    }

    public UserResponse updateDetails(Long id, UserDetailsUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        userRepository.findByEmail(request.getEmail())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ValidationException("Email '" + request.getEmail() + "' is already registered");
                });

        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setAddress(request.getAddress());
        user.setPhone(request.getPhone());
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
