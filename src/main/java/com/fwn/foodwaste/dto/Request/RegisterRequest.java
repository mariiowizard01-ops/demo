package com.fwn.foodwaste.dto.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be 3–30 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // donor-specific optional fields used when registering as a donor
    private String donorName;
    private String address;
    @Size(min = 10, max = 10, message = "Phone number must be exactly 10 digits")
    private String phone;
    private List<Long> collectionCenterIds;

    // optional — if omitted, defaults to ROLE_DONOR
    private Set<String> roles;
}
