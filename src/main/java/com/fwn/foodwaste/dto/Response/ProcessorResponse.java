package com.fwn.foodwaste.dto.Response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessorResponse {
    private Long id;
    private String name;
    private String location;
    private Double maxProcessingCapacityKg;
    private Double currentLoadKg;
    private Double freeCapacityKg;        // derived
    private int assignedCentersCount;
    private LocalDateTime createdAt;
}
