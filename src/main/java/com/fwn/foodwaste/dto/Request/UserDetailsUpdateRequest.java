package com.fwn.foodwaste.dto.Request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDetailsUpdateRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;

    @Size(min = 2, max = 100,
            message = "Name must be between 2 and 100 characters")
    @Pattern(
            regexp  = "^[a-zA-Z ]+$",
            message = "Name can only contain letters and spaces"
    )
    @Column(nullable = true)
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
    private String phone;
}