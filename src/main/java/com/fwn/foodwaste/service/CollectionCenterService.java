package com.fwn.foodwaste.service;

import com.fwn.foodwaste.dto.Request.CollectionCenterRequest;
import com.fwn.foodwaste.dto.Response.CollectionCenterResponse;
import com.fwn.foodwaste.dto.Response.FoodWasteItemResponse;
import com.fwn.foodwaste.entity.CollectionCentres;
import com.fwn.foodwaste.entity.FoodWasteItems;
import com.fwn.foodwaste.entity.Processors;
import com.fwn.foodwaste.exception.CapacityExceededException;
import com.fwn.foodwaste.exception.ResourceNotFoundException;
import com.fwn.foodwaste.exception.ValidationException;
import com.fwn.foodwaste.repository.CollectionCenterRepository;
import com.fwn.foodwaste.repository.FoodWasteItemRepository;
import com.fwn.foodwaste.repository.ProcessorRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.management.Query;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CollectionCenterService {

    private final ProcessorLoadBalancerService loadBalancer;
    private final CollectionCenterRepository centerRepo;
    private final ProcessorRepository processorRepo;
    private final FoodWasteItemRepository itemRepo;

    @Transactional(readOnly = true)
    public List<CollectionCenterResponse> findAll() {
        return centerRepo.findAll()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CollectionCenterResponse findById(Long id) {
        return toResponse(getCenter(id));
    }

    public CollectionCenterResponse create(CollectionCenterRequest req) {
        CollectionCentres center = new CollectionCentres();
        mapFields(center, req);
        return toResponse(centerRepo.save(center));
    }

    public CollectionCenterResponse update(Long id,
                                           CollectionCenterRequest req) {
        CollectionCentres center = getCenter(id);
        mapFields(center, req);
        return toResponse(centerRepo.save(center));
    }

    public void delete(Long id) {
        CollectionCentres center = centerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Collection center not found: " + id));

        if (center.getProcessor() != null) {
            throw new ValidationException(
                    "This collection center cannot be deleted because it is assigned to a processor. "
                            + "Reassign or remove the processor first.");
        }
        if (!center.getDonors().isEmpty()) {
            throw new ValidationException(
                    "This collection center cannot be deleted because donors are assigned to it. "
                            + "Remove the donor assignments first.");
        }
        if (!center.getFoodWasteItems().isEmpty()) {
            throw new ValidationException(
                    "This collection center cannot be deleted because it has donated food items. "
                            + "Remove or reassign those items first.");
        }

        centerRepo.deleteById(id);
    }




//    END-OF-DAY DISPATCH


//    public String dispatchToProcessor(Long centerId) {
//
//        CollectionCentres center = getCenter(centerId);
//
//        List<FoodWasteItems> approvedItems = itemRepo
//                .findByCollectionCentre_IdAndAcceptedTrueAndRejectedFalseAndDispatchedFalseOrderByExpirationDateAsc(centerId);
//
//        if (approvedItems.isEmpty())
//            return "No accepted items ready to dispatch at '" + center.getLocation() + "'";
//
//        int dispatchedCount = 0;
//        double totalKg = 0.0;
//        String lastProcessor ="";
//        double totalKg = approvedItems.stream()
//                .mapToDouble(FoodWasteItems::getWeightKg).sum();
//
//        Processors processor = loadBalancer.findBestProcessor(totalKg);
//
//        approvedItems.forEach(item -> item.setDispatched(true));
//        itemRepo.saveAll(approvedItems);
//
//        processor.setCurrentLoadKg(
//                processor.getCurrentLoadKg() + totalKg);
//        processorRepo.save(processor);
//
//        center.setCurrentLoadKg(0.0);
//        centerRepo.save(center);
//
//        return "Dispatched " + approvedItems.size()
//                + " accepted items (" + totalKg + " kg)"
//                + " to '" + processor.getName() + "'";
//    }

    public String dispatchToProcessor(Long centerId) {

        CollectionCentres center = getCenter(centerId);

        // FEFO — fetch accepted items sorted by expiration date ASC
        // earliest expiring items are dispatched first
        Queue<FoodWasteItems> queue = new PriorityQueue<>(
                Comparator.comparing(FoodWasteItems::getExpirationDate)
                        .thenComparing(FoodWasteItems::getId)
        );
        queue.addAll(itemRepo
                .findByCollectionCentre_IdAndAcceptedTrueAndRejectedFalseAndDispatchedFalseAndProcessedFalseOrderByExpirationDateAscIdAsc(
                        centerId));

        if (queue.isEmpty())
            return "No accepted items ready to dispatch at '"
                    + center.getLocation() + "'";

        int dispatchedCount = 0;
        double totalKg      = 0.0;
        String lastProcessor = "";

        // Process each item one by one in FEFO order
        // Load balancer picks the best processor for EACH item separately
//        for (FoodWasteItems item : approvedItems) {

            // Load balancer picks processor with lowest utilization
            // that can still fit this item's weight
//            Processors processor =
//                    loadBalancer.findBestProcessor(item.getWeightKg());
//
//            item.setDispatched(true);
//            item.setProcessor(processor);
//            itemRepo.save(item);
//
//            processor.setCurrentLoadKg(
//                    processor.getCurrentLoadKg() + item.getWeightKg());
//            processorRepo.save(processor);
//
//            dispatchedCount++;
//            totalKg      += item.getWeightKg();
//            lastProcessor = processor.getName();
        while (!queue.isEmpty()) {
            FoodWasteItems item = queue.poll(); // FEFO dequeue

            Processors processor = loadBalancer.findBestProcessor(item.getWeightKg());

            item.setDispatched(true);
            item.setProcessor(processor);
            itemRepo.save(item);

            processor.setCurrentLoadKg(processor.getCurrentLoadKg() + item.getWeightKg());
            processorRepo.save(processor);

            dispatchedCount++;
            totalKg += item.getWeightKg();
            lastProcessor = processor.getName();
        }

        // Reset center load after full dispatch
        center.setCurrentLoadKg(0.0);
        centerRepo.save(center);

        return "Dispatched " + dispatchedCount
                + " items (" + totalKg + " kg)"
                + " in FEFO order"
                + " using load-balanced processor selection.";
    }

    //for single collection center dispatch by fefo
        public String dispatchSingleItem(Long centerId, Long itemId) {
                CollectionCentres center = getCenter(centerId);

            Queue<FoodWasteItems> queue = new PriorityQueue<>(
                    Comparator.comparing(FoodWasteItems::getExpirationDate)
                            .thenComparing(FoodWasteItems::getId)
            );
            queue.addAll(itemRepo
                            .findByCollectionCentre_IdAndAcceptedTrueAndRejectedFalseAndDispatchedFalseAndProcessedFalseOrderByExpirationDateAscIdAsc(
                                            centerId));


            if (queue.isEmpty()) {
                return "No accepted items ready to dispatch at this center.";
            }
                        FoodWasteItems item = queue.peek();
                        if (!item.getId().equals(itemId)) {
                                throw new ValidationException("Dispatch waste in earliest-expiry order. Item "
                                                                + item.getId() + " expires first.");
                        }
                        queue.poll(); // FEFO dequeue
            Processors processor = loadBalancer.findBestProcessor(item.getWeightKg());

//                if (item.getCollectionCentre() == null
//                                || !item.getCollectionCentre().getId().equals(centerId)
//                                || !item.isAccepted() || item.isRejected() || item.isDispatched()) {
//                        throw new ValidationException("Only accepted, undispatched items from this center can be dispatched.");
//                }

//                Processors processor = loadBalancer.findBestProcessor(item.getWeightKg());
                item.setDispatched(true);
                item.setProcessor(processor);
                itemRepo.save(item);
                processor.setCurrentLoadKg(processor.getCurrentLoadKg() + item.getWeightKg());
                processorRepo.save(processor);

                return "Dispatched item " + item.getId() + " (" + item.getWeightKg() + " kg)"
                                + " to '" + processor.getName() + "'";
        }

    private void mapFields(CollectionCentres c,
                           CollectionCenterRequest req) {
        if (req.getProcessorId() != null) {
            Processors processor = processorRepo.findById(req.getProcessorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Processor not found: " + req.getProcessorId()));
            if (req.getMaxCapacityKg() > processor.getMaxProcessingCapicityKg()) {
                throw new ValidationException(
                        "Collection center capacity cannot exceed its assigned processor capacity ("
                                + processor.getMaxProcessingCapicityKg() + " kg).");
            }
            c.setProcessor(processor);
        }

        c.setName(req.getName());
        c.setLocation(req.getLocation());
        c.setMaxCapicityKg(req.getMaxCapacityKg());
    }

    public CollectionCentres getCenter(Long id) {
        return centerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Collection center not found: " + id));
    }

    public CollectionCenterResponse toResponse(CollectionCentres c) {
        double activeLoad = itemRepo.findByCollectionCentre_Id(c.getId()).stream()
                .filter(item -> item.isAccepted() && !item.isRejected() && !item.isDispatched())
                .mapToDouble(FoodWasteItems::getWeightKg)
                .sum();
        double pct = c.getMaxCapicityKg() > 0
                ? (activeLoad / c.getMaxCapicityKg()) * 100
                : 0;
        int pending = itemRepo
                .findByCollectionCentre_IdAndProcessedFalse(c.getId())
                .size();

        return CollectionCenterResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .location(c.getLocation())
                .maxCapacityKg(c.getMaxCapicityKg())
                .currentLoadKg(activeLoad)
                .capacityUsedPercent(Math.round(pct * 10.0) / 10.0)
                .processorName(c.getProcessor() != null
                        ? c.getProcessor().getName() : null)
                .processorId(c.getProcessor() != null
                        ? c.getProcessor().getId() : null)
                .pendingItemsCount(pending)
                .createdAt(c.getCreatedAt())
                .build();
    }

}
