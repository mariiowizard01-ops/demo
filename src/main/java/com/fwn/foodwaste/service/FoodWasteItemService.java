package com.fwn.foodwaste.service;

import com.fwn.foodwaste.dto.Request.AutoAssignFoodWasteItemRequest;
import com.fwn.foodwaste.dto.Request.FoodWasteItemRequest;
import com.fwn.foodwaste.dto.Response.FoodWasteItemResponse;
import com.fwn.foodwaste.entity.CollectionCentres;
import com.fwn.foodwaste.entity.FoodWasteItems;
import com.fwn.foodwaste.entity.Processors;
import com.fwn.foodwaste.entity.User;
import com.fwn.foodwaste.entity.enums.WasteType;
import com.fwn.foodwaste.exception.CapacityExceededException;
import com.fwn.foodwaste.exception.ResourceNotFoundException;
import com.fwn.foodwaste.exception.ValidationException;
import com.fwn.foodwaste.repository.CollectionCenterRepository;
import com.fwn.foodwaste.repository.FoodWasteItemRepository;
import com.fwn.foodwaste.repository.UserRepository;
import com.fwn.foodwaste.repository.ProcessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FoodWasteItemService {
    private final GreedyCollectionCenterService greedyService;
    private final FoodWasteItemRepository itemRepo;
    private final UserRepository donorRepo;
    private final CollectionCenterRepository centerRepo;
    private final ProcessorRepository processorRepo;


    // ADD this map as a constant at the top of the class
// Key = WasteType, Value = maximum allowed days until expiration
    private static final Map<WasteType, Integer> MAX_EXPIRY_DAYS = Map.of(
            WasteType.VEGETABLES,  7,    // vegetables expire in max 7 days
            WasteType.MEAT,        5,    // meat expires in max 5 days
            WasteType.DAIRY,       10,   // dairy expires in max 10 days
            WasteType.FRUITS,      7,    // fruits expire in max 7 days
            WasteType.GRAINS,      180,  // grains can last 6 months
            WasteType.BEVERAGES,   90,   // beverages last 3 months
            WasteType.OTHER,       30    // default 30 days for unknown
    );

    private void validateExpirationDateForWasteType(
            WasteType wasteType, LocalDate expirationDate) {

        long daysUntilExpiry = ChronoUnit.DAYS.between(
                LocalDate.now(), expirationDate);

        Integer maxDays = MAX_EXPIRY_DAYS.get(wasteType);

        if (maxDays != null && daysUntilExpiry > maxDays) {
            throw new ValidationException(
                    "Expiration date is not realistic for waste type '"
                            + wasteType + "'. "
                            + "Maximum allowed days until expiry: " + maxDays
                            + " days. You entered: " + daysUntilExpiry + " days.");
        }
    }


    @Transactional(readOnly = true)
    public List<FoodWasteItemResponse> findAll() {
                if (isDonorCaller()) {
                        return itemRepo.findByDonorId(getAuthenticatedDonor().getId())
                                        .stream().map(this::toResponse)
                                        .collect(Collectors.toList());
                }
        return itemRepo.findAll()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FoodWasteItemResponse findById(Long id) {
                FoodWasteItems item = getItem(id);
                requireDonorOwnership(item.getDonor());
                return toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<FoodWasteItemResponse> findByDonor(Long donorId) {
                if (isDonorCaller() && !getAuthenticatedDonor().getId().equals(donorId)) {
                        throw new AccessDeniedException("You can only view your own donations.");
                }
        return itemRepo.findByDonorId(donorId)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FoodWasteItemResponse> findByCenter(Long centerId) {
        return itemRepo.findByCollectionCentre_Id(centerId)
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Priority-queue ordered list (soonest expiry first)
    @Transactional(readOnly = true)
    public List<FoodWasteItemResponse> getProcessingQueue() {
        return itemRepo.findByProcessedFalseOrderByExpirationDateAsc()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    public FoodWasteItemResponse create(FoodWasteItemRequest req) {
        User donor = donorRepo.findById(req.getDonorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Donor not found: " + req.getDonorId()));
        requireDonorOwnership(donor);

        CollectionCentres center = centerRepo.findById(req.getCollectionCenterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Collection center not found: "
                                + req.getCollectionCenterId()));
        requireDonorCenterAssignment(donor, center);

        validateExpirationDateForWasteType(req.getWasteType(),
                req.getExpirationDate());
        // capacity check
        if (activeCenterLoad(center) + req.getWeightKg() > center.getMaxCapicityKg())
            throw new CapacityExceededException(
                    "Center '" + center.getLocation() + "' is full. "
                            + "Max: " + center.getMaxCapicityKg() + " kg, "
                            + "Current: " + center.getCurrentLoadKg() + " kg");

        FoodWasteItems item = new FoodWasteItems();
        item.setWeightKg(req.getWeightKg());
        item.setExpirationDate(req.getExpirationDate());
        item.setWasteType(req.getWasteType());
        item.setDonor(donor);
        item.setCollectionCentre(center);
        item.setAccepted(false);
        item.setProcessed(false);

        return toResponse(itemRepo.save(item));
    }

    public FoodWasteItemResponse update(Long id, FoodWasteItemRequest req) {
        FoodWasteItems item = getItem(id);
        requireDonorOwnership(item.getDonor());

                boolean isDonor = isDonorCaller();
                if (isDonor && (item.isAccepted() || item.isProcessed() || item.isRejected() || item.isDispatched())) {
                        throw new ValidationException("Accepted or rejected waste items cannot be edited by a donor.");
                }
                if (isDonor && (req.getProcessed() != null || req.getRejected() != null)) {
                        throw new ValidationException("Donors cannot change the acceptance status of a waste item.");
                }
                requireDonorCenterAssignment(item.getDonor(), item.getCollectionCentre());

        // if center changed, adjust loads on both old and new center
        if (!item.getCollectionCentre().getId()
                .equals(req.getCollectionCenterId())) {

            CollectionCentres oldCenter = item.getCollectionCentre();
            oldCenter.setCurrentLoadKg(
                    oldCenter.getCurrentLoadKg() - item.getWeightKg());
            centerRepo.save(oldCenter);

            CollectionCentres newCenter = centerRepo
                    .findById(req.getCollectionCenterId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Collection center not found: "
                                    + req.getCollectionCenterId()));
            requireDonorCenterAssignment(item.getDonor(), newCenter);

            if (!newCenter.hasCapacity(req.getWeightKg()))
                throw new CapacityExceededException(
                        "Target center is full");

            newCenter.setCurrentLoadKg(
                    newCenter.getCurrentLoadKg() + req.getWeightKg());
            centerRepo.save(newCenter);
            item.setCollectionCentre(newCenter);
        }

        item.setWeightKg(req.getWeightKg());
        item.setExpirationDate(req.getExpirationDate());
        item.setWasteType(req.getWasteType());

        if (req.getProcessed() != null) {
            item.setProcessed(req.getProcessed());
        }

        if (req.getRejected() != null) {
            item.setRejected(req.getRejected());
            if (req.getRejected()) {
                item.setProcessed(false);
            }
        }

        FoodWasteItemResponse response = toResponse(itemRepo.save(item));
        refreshCenterLoad(item.getCollectionCentre());
        return response;
    }

    public FoodWasteItemResponse accept(Long id) {
        FoodWasteItems item = getItem(id);
                if (item.isAccepted() || item.isRejected() || item.isDispatched() || item.isProcessed()) {
                        throw new ValidationException("Only pending waste can be accepted.");
                }
                List<FoodWasteItems> pendingAtCenter = itemRepo
                                .findByCollectionCentre_IdAndAcceptedFalseAndRejectedFalseAndDispatchedFalseAndProcessedFalseOrderByExpirationDateAscIdAsc(
                                                item.getCollectionCentre().getId());
                if (pendingAtCenter.isEmpty()) {
                        throw new ValidationException("No pending waste is available to accept at this center.");
                }
                if (!pendingAtCenter.get(0).getId().equals(item.getId())) {
                        throw new ValidationException("Accept waste in earliest-expiry order. Item "
                                        + pendingAtCenter.get(0).getId() + " expires first.");
                }
                if (activeCenterLoad(item.getCollectionCentre()) + item.getWeightKg() > item.getCollectionCentre().getMaxCapicityKg()) {
                        throw new CapacityExceededException("Collection center is full. Dispatch accepted waste before accepting more.");
                }
                item.setAccepted(true);
                item.setProcessed(false);
        item.setRejected(false);
        FoodWasteItemResponse response = toResponse(itemRepo.save(item));
        refreshCenterLoad(item.getCollectionCentre());
        return response;
    }

    public FoodWasteItemResponse reject(Long id) {
        FoodWasteItems item = getItem(id);
                if (item.isAccepted() || item.isRejected() || item.isDispatched() || item.isProcessed()) {
                        throw new ValidationException("Only pending waste can be rejected.");
                }
                item.setAccepted(false);
        item.setProcessed(false);
        item.setRejected(true);
        FoodWasteItemResponse response = toResponse(itemRepo.save(item));
        refreshCenterLoad(item.getCollectionCentre());
        return response;
    }

        public FoodWasteItemResponse completeProcessing(Long id) {
                FoodWasteItems item = getItem(id);
                if (!item.isAccepted() || !item.isDispatched() || item.isRejected() || item.isProcessed()
                                || item.getProcessor() == null) {
                        throw new ValidationException("Only accepted and dispatched waste can be marked as processed.");
                }
                List<FoodWasteItems> processorQueue = itemRepo
                                .findByProcessor_IdAndAcceptedTrueAndRejectedFalseAndDispatchedTrueAndProcessedFalseOrderByExpirationDateAscIdAsc(
                                                item.getProcessor().getId());
                if (processorQueue.isEmpty()) {
                        throw new ValidationException("No dispatched waste is available for this processor.");
                }
                if (!processorQueue.get(0).getId().equals(item.getId())) {
                        throw new ValidationException("Process waste in earliest-expiry order. Item "
                                        + processorQueue.get(0).getId() + " expires first.");
                }
                item.setProcessed(true);
                // using the processor that is actually assigned
                if (item.getProcessor() != null ) {
                        Processors processor = item.getProcessor();
                        processor.setCurrentLoadKg(Math.max(0.0,
                                processor.getCurrentLoadKg() - item.getWeightKg()));
                        processorRepo.save(processor);
                }
                return toResponse(itemRepo.save(item));
        }

    public void delete(Long id) {
        FoodWasteItems item = getItem(id);
        // give capacity back to the center
        CollectionCentres center = item.getCollectionCentre();
        if (center != null && !item.isProcessed()) {
            center.setCurrentLoadKg(
                    center.getCurrentLoadKg() - item.getWeightKg());
            centerRepo.save(center);
        }
        itemRepo.deleteById(id);
    }

    private FoodWasteItems getItem(Long id) {
        return itemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Food waste item not found: " + id));
    }

        private boolean isDonorCaller() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                return authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(authority -> "ROLE_DONOR".equals(authority.getAuthority()));
        }

        private User getAuthenticatedDonor() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null) {
                        throw new AccessDeniedException("Authentication is required.");
                }
                return donorRepo.findByUsername(authentication.getName())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Donor account not found: " + authentication.getName()));
        }

        private void requireDonorOwnership(User donor) {
                if (isDonorCaller() && !getAuthenticatedDonor().getId().equals(donor.getId())) {
                        throw new AccessDeniedException("You can only access your own donations.");
                }
        }

        private void requireDonorCenterAssignment(User donor, CollectionCentres center) {
                if (isDonorCaller()) {
                        requireDonorOwnership(donor);
                        boolean assigned = donor.getCollectionCentres().stream()
                                        .anyMatch(assignedCenter -> assignedCenter.getId().equals(center.getId()));
                        if (!assigned) {
                                throw new AccessDeniedException(
                                                "You can only submit donations to centers assigned to your account.");
                        }
                }
        }

        private double activeCenterLoad(CollectionCentres center) {
                return itemRepo.findByCollectionCentre_Id(center.getId()).stream()
                                .filter(item -> item.isAccepted() && !item.isRejected() && !item.isDispatched())
                                .mapToDouble(FoodWasteItems::getWeightKg)
                                .sum();
        }

        private void refreshCenterLoad(CollectionCentres center) {
                if (center == null) return;
                center.setCurrentLoadKg(activeCenterLoad(center));
                centerRepo.save(center);
        }

    // auto assign method that uses greedy approach to select best collection centers
    public FoodWasteItemResponse createWithAutoAssign(
            AutoAssignFoodWasteItemRequest req) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isDonor = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_DONOR".equals(authority.getAuthority()));
        User donor;
        if (isDonor) {
            donor = donorRepo.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Donor account not found: " + authentication.getName()));
        } else {
            donor = donorRepo.findById(req.getDonorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Donor not found: " + req.getDonorId()));
        }

        validateExpirationDateForWasteType(req.getWasteType(),
                req.getExpirationDate());

        // Select the best-capacity center only after validating the donor and item.
        CollectionCentres bestCenter = isDonor
                ? greedyService.findBestCenter(req.getWeightKg(), donor.getCollectionCentres())
                : greedyService.findBestCenter(req.getWeightKg());

        FoodWasteItems item = FoodWasteItems.builder()
                .weightKg(req.getWeightKg())
                .expirationDate(req.getExpirationDate())
                .wasteType(req.getWasteType())
                .processed(false)
                .accepted(false)
                .rejected(false)
                .dispatched(false)
                .donor(donor)
                .collectionCentre(bestCenter)
                .build();
// item starts as accepted = false so it should NOT count toward the active center load yet. The load updates when the operator calls accept()
//        bestCenter.setCurrentLoadKg(
//                bestCenter.getCurrentLoadKg() + req.getWeightKg());
//        centerRepo.save(bestCenter);

        return toResponse(itemRepo.save(item));
    }

    public FoodWasteItemResponse toResponse(FoodWasteItems i) {
        long daysLeft = ChronoUnit.DAYS.between(
                LocalDate.now(), i.getExpirationDate());

        return FoodWasteItemResponse.builder()
                .id(i.getId())
                .weightKg(i.getWeightKg())
                .expirationDate(i.getExpirationDate())
                .wasteType(i.getWasteType())
                .accepted(i.isAccepted())
                .processed(i.isProcessed())
                .rejected(i.isRejected())
                .dispatched(i.isDispatched())
                .donorName(i.getDonor().getName())
                .donorId(i.getDonor().getId())
                .collectionCenterLocation(
                        i.getCollectionCentre() != null
                                ? i.getCollectionCentre().getLocation() : null)
                .collectionCenterId(
                        i.getCollectionCentre() != null
                                ? i.getCollectionCentre().getId() : null)
                .daysUntilExpiry(daysLeft)
                .createdAt(i.getCreatedAt())
                .build();
    }
}
