package com.fwn.foodwaste.dto.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(
            regexp  = "^(?=.*[a-zA-Z])[a-zA-Z0-9_-]+$",
            message = "Username must contain at least one letter. "
                    + "Pure numbers are not valid usernames."
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // donor-specific optional fields used when registering as a donor
    @Size(min = 2, max = 100,
            message = "Name must be between 2 and 100 characters")
    @Pattern(
            regexp  = "^[a-zA-Z ]+$",
            message = "Name can only contain letters and spaces"
    )
    private String name;
    @Size(min = 5, max = 255,
            message = "Address must be between 5 and 255 characters")
    @Pattern(
            regexp  = "^[a-zA-Z0-9\\s,.-]*$",
            message = "Address can only contain letters, numbers, "
                    + "spaces, commas, hyphens and dots"
    )
    private String address;

    @Pattern(
            regexp  = "^\\+?[0-9]{7,15}$",
            message = "Phone must be 7 to 15 digits, "
                    + "optionally prefixed with +"
    )
    @Size(min = 10, max = 10, message = "Phone number must be exactly 10 digits")
    private String phone;
    private List<Long> collectionCenterIds;

    // optional — if omitted, defaults to ROLE_DONOR
    private Set<String> roles;
}
