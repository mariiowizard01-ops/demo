package com.fwn.foodwaste.dto.Response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionCenterResponse {
    private Long id;
    private String name;
    private String location;
    private Double maxCapacityKg;
    private Double currentLoadKg;
    private Double capacityUsedPercent;   // derived
    private String processorName;
    private Long processorId;
    private int pendingItemsCount;
    private LocalDateTime createdAt;
}
