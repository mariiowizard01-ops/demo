package com.fwn.foodwaste.dto.Response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
    private boolean active;
    private String name;
    private String address;
    private String phone;
    private int totalDonations;
    private List<String> collectionCenterLocations;
    private LocalDateTime createdAt;
}
