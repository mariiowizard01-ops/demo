package com.fwn.foodwaste.dto.Response;

import com.fwn.foodwaste.entity.enums.WasteType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FoodWasteItemResponse {

    private Long id;
    private Double weightKg;
    private LocalDate expirationDate;
    private WasteType wasteType;
    private boolean accepted;
    private boolean processed;
    private boolean rejected;
    private boolean dispatched;
    private String donorName;
    private Long donorId;
    private String collectionCenterLocation;
    private Long collectionCenterId;
    private long daysUntilExpiry;       // derived
    private LocalDateTime createdAt;

}
