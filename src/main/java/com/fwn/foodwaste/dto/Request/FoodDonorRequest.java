package com.fwn.foodwaste.dto.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FoodDonorRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Must be a valid email")
    private String contactEmail;

    @Pattern(regexp = "^\\d{10}$",
            message = "Phone number must be exactly 10 digits")
    private String contactPhone;

    // IDs of collection centers this donor delivers to
    private List<Long> collectionCenterIds;
}
