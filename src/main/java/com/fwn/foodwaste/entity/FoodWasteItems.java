package com.fwn.foodwaste.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fwn.foodwaste.entity.enums.WasteType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "food_waste_item")
@Builder
public class FoodWasteItems extends BaseEntity{

    @NotNull
    @Positive
    @Column(nullable = false)
    private Double weightKg;

    @NotNull
    @Future(message = "expiration data most be in future")
    @Column(nullable = false)
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    private WasteType wasteType;

    private boolean accepted = false;
    private boolean processed = false;
    private boolean rejected = false;
    private boolean dispatched = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User donor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_center_id")
    private CollectionCentres collectionCentre;
}
