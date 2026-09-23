package com.fwn.foodwaste.dto.Request;

import com.fwn.foodwaste.entity.enums.WasteType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class CollectionCenterRequest {

    @NotBlank(message = "name is required")
    private String name;
    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Max capacity is required")
    @Positive(message = "Capacity must be positive")
    private Double maxCapacityKg;

    // nullable on creation — assign processor later
    private Long processorId;
}


