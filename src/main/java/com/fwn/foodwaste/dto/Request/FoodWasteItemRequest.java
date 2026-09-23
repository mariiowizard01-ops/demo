package com.fwn.foodwaste.dto.Request;

import com.fwn.foodwaste.entity.enums.WasteType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FoodWasteItemRequest {

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    private Double weightKg;

    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;

    @NotNull(message = "Waste type is required")
    private WasteType wasteType;

    @NotNull(message = "Donor ID is required")
    private Long donorId;

    @NotNull(message = "Collection center ID is required")
    private Long collectionCenterId;

    private Boolean processed;
    private Boolean rejected;
}
