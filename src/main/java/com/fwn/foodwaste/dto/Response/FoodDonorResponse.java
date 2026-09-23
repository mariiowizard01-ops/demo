package com.fwn.foodwaste.dto.Response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodDonorResponse {
    private Long id;

    private String name;

    private String address;

    private String contactEmail;
    private String contactPhone;
    private List<String> collectionCenterLocations;
    private int totalDonations;
    private LocalDateTime createdAt;
}
