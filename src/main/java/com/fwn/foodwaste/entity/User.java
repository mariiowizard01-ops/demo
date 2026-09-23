package com.fwn.foodwaste.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class User extends BaseEntity{

    @NotBlank
    @Column(nullable = false, unique = true)
    private String username;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
        )
    private Set<Role> roles = new HashSet<>();

    private boolean active = true;

    private String name;
    private String address;
    private String phone;

    @ManyToMany
    @JoinTable(name = "donor_collection_center",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "center_id"))
    private List<CollectionCentres> collectionCentres = new ArrayList<>();

    @OneToMany(mappedBy = "donor")
    private List<FoodWasteItems> foodWasteItems = new ArrayList<>();

}
