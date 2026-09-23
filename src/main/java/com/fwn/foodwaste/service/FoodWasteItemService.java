package com.fwn.foodwaste.service;

import com.fwn.foodwaste.dto.Request.AutoAssignFoodWasteItemRequest;
import com.fwn.foodwaste.dto.Request.FoodWasteItemRequest;
import com.fwn.foodwaste.dto.Response.FoodWasteItemResponse;
import com.fwn.foodwaste.entity.CollectionCentres;
import com.fwn.foodwaste.entity.FoodWasteItems;
import com.fwn.foodwaste.entity.User;
import com.fwn.foodwaste.exception.CapacityExceededException;
import com.fwn.foodwaste.exception.ResourceNotFoundException;
import com.fwn.foodwaste.exception.ValidationException;
import com.fwn.foodwaste.repository.CollectionCenterRepository;
import com.fwn.foodwaste.repository.FoodWasteItemRepository;
import com.fwn.foodwaste.repository.UserRepository;
import com.fwn.foodwaste.repository.ProcessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    @Transactional(readOnly = true)
    public List<FoodWasteItemResponse> findAll() {
        return itemRepo.findAll()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FoodWasteItemResponse findById(Long id) {
        return toResponse(getItem(id));
    }

    @Transactional(readOnly = true)
    public List<FoodWasteItemResponse> findByDonor(Long donorId) {
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

        CollectionCentres center = centerRepo.findById(req.getCollectionCenterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Collection center not found: "
                                + req.getCollectionCenterId()));

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

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                boolean isDonor = authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(authority -> "ROLE_DONOR".equals(authority.getAuthority()));
                if (isDonor && (item.isAccepted() || item.isProcessed() || item.isRejected() || item.isDispatched())) {
                        throw new ValidationException("Accepted or rejected waste items cannot be edited by a donor.");
                }
                if (isDonor && (req.getProcessed() != null || req.getRejected() != null)) {
                        throw new ValidationException("Donors cannot change the acceptance status of a waste item.");
                }

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
                if (!item.isAccepted() || !item.isDispatched() || item.isRejected()) {
                        throw new ValidationException("Only accepted and dispatched waste can be marked as processed.");
                }
                item.setProcessed(true);
                if (item.getCollectionCentre() != null && item.getCollectionCentre().getProcessor() != null) {
                        var processor = item.getCollectionCentre().getProcessor();
                        processor.setCurrentLoadKg(Math.max(0.0, processor.getCurrentLoadKg() - item.getWeightKg()));
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

        // Greedy picks the best center — no centerId needed from clients
        CollectionCentres bestCenter =
                greedyService.findBestCenter(req.getWeightKg());

        User donor = donorRepo.findById(req.getDonorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Donor not found: " + req.getDonorId()));

        FoodWasteItems item = FoodWasteItems.builder()
                .weightKg(req.getWeightKg())
                .expirationDate(req.getExpirationDate())
                .wasteType(req.getWasteType())
                .processed(false)
                .donor(donor)
                .collectionCentre(bestCenter)
                .build();

        bestCenter.setCurrentLoadKg(
                bestCenter.getCurrentLoadKg() + req.getWeightKg());
        centerRepo.save(bestCenter);

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
