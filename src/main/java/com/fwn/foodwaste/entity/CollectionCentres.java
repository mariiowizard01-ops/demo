package com.fwn.foodwaste.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "collection_centres")

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionCentres extends BaseEntity{

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String location;

    @Positive
    @Column(nullable = false)
    private Double maxCapicityKg;

    @Column(nullable = false)
    private Double currentLoadKg = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private Processors processor;

    @ManyToMany(mappedBy = "collectionCentres")
    private List<User> donors = new ArrayList<>();

    @OneToMany(mappedBy = "collectionCentre", cascade = CascadeType.ALL)
    private List<FoodWasteItems> foodWasteItems = new ArrayList<>();

    public boolean hasCapacity (double weighKg){
        return (currentLoadKg + weighKg) <= maxCapicityKg;
    }
}
