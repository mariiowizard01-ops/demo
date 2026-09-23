package com.fwn.foodwaste.service;

import com.fwn.foodwaste.dto.Request.FoodDonorRequest;
import com.fwn.foodwaste.dto.Response.FoodDonorResponse;
import com.fwn.foodwaste.entity.CollectionCentres;
import com.fwn.foodwaste.entity.User;
import com.fwn.foodwaste.entity.enums.RoleName;
import com.fwn.foodwaste.exception.ResourceNotFoundException;
import com.fwn.foodwaste.exception.ValidationException;
import com.fwn.foodwaste.repository.CollectionCenterRepository;
import com.fwn.foodwaste.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FoodDonorService {

    private final UserRepository userRepo;
    private final CollectionCenterRepository centerRepo;

    @Transactional(readOnly = true)
    public List<FoodDonorResponse> findAll() {
        return userRepo.findByRoles_Role(RoleName.ROLE_DONOR)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FoodDonorResponse findById(Long id) {
        return toResponse(getDonor(id));
    }

    public FoodDonorResponse create(FoodDonorRequest req) {
        User donor = userRepo.findByEmail(req.getContactEmail())
                .orElseThrow(() -> new ValidationException("Donor signup must create the user account first."));
        if (donor.getRoles().stream().noneMatch(role -> role.getRole() == RoleName.ROLE_DONOR)) {
            throw new ValidationException("Only donor users can create a donor profile.");
        }
        mapFields(donor, req);
        return toResponse(userRepo.save(donor));
    }

    public FoodDonorResponse update(Long id, FoodDonorRequest req) {
        User donor = getDonor(id);
        mapFields(donor, req);
        return toResponse(userRepo.save(donor));
    }

    public void delete(Long id) {
        if (!userRepo.existsById(id)) {
            throw new ResourceNotFoundException("Donor not found: " + id);
        }
        userRepo.deleteById(id);
    }

    private void mapFields(User donor, FoodDonorRequest req) {
        donor.setName(req.getName());
        donor.setAddress(req.getAddress());
        donor.setPhone(req.getContactPhone());

        if (req.getCollectionCenterIds() != null) {
            List<CollectionCentres> centers = req.getCollectionCenterIds().stream()
                    .map(id -> centerRepo.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Collection center not found: " + id)))
                    .collect(Collectors.toList());
            donor.setCollectionCentres(centers);
        }
    }

    private User getDonor(Long id) {
        User donor = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donor not found: " + id));
        if (donor.getRoles().stream().noneMatch(role -> role.getRole() == RoleName.ROLE_DONOR)) {
            throw new ResourceNotFoundException("Donor not found: " + id);
        }
        return donor;
    }

    private FoodDonorResponse toResponse(User donor) {
        return FoodDonorResponse.builder()
                .id(donor.getId())
                .name(donor.getName() != null ? donor.getName() : donor.getUsername())
                .address(donor.getAddress())
                .contactEmail(donor.getEmail())
                .contactPhone(donor.getPhone())
                .collectionCenterLocations(donor.getCollectionCentres().stream()
                        .map(CollectionCentres::getLocation).collect(Collectors.toList()))
                .totalDonations(donor.getFoodWasteItems().size())
                .createdAt(donor.getCreatedAt())
                .build();
    }
}
