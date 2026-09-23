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
public class FoodItemRequest {
    @NotNull
    @Positive
    private Double weightKg;

    @NotNull @Future
    private LocalDate expirationDate;

    @NotNull
    private WasteType wasteType;

    @NotNull
    private Long donorId;

    @NotNull
    private Long collectionCenterId;
}
