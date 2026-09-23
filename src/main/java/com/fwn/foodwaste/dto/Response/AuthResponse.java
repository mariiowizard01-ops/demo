package com.fwn.foodwaste.dto.Response;

import lombok.*;

import java.util.Set;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;

    private String type = "Bearer";

    private Long id;

    private String username;

    private String email;

    private Set<String> roles;
}
