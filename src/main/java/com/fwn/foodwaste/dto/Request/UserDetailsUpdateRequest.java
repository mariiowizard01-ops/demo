package com.fwn.foodwaste.dto.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDetailsUpdateRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;

    private String name;
    private String address;
    private String phone;
}