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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
        if (!centerRepo.existsById(id))
            throw new ResourceNotFoundException(
                    "Collection center not found: " + id);
        centerRepo.deleteById(id);
    }




//    END-OF-DAY DISPATCH
//    public String dispatchToProcessor(Long centerId) {
//        CollectionCentres center = getCenter(centerId);
//
//        if (center.getProcessor() == null)
//            throw new ValidationException(
//                    "No processor assigned to center '"
//                            + center.getLocation() + "'");
//
//        List<FoodWasteItems> pending =
//                itemRepo.findByCollectionCentre_IdAndProcessedFalse(centerId);
//
//        if (pending.isEmpty())
//            return "No pending items at '" + center.getLocation() + "'";
//
//        double totalKg = pending.stream()
//                .mapToDouble(FoodWasteItems::getWeightKg).sum();
//
//        Processors processor = center.getProcessor();
//        if (processor.getFreeCapacity() < totalKg)
//            throw new CapacityExceededException(
//                    "Processor '" + processor.getName()
//                            + "' cannot accept " + totalKg + " kg. "
//                            + "Free: " + processor.getFreeCapacity() + " kg.");
//
//        pending.forEach(i -> i.setProcessed(true));
//        itemRepo.saveAll(pending);
//        processor.setCurrentLoadKg(processor.getCurrentLoadKg() + totalKg);
//        processorRepo.save(processor);
//        center.setCurrentLoadKg(0.0);
//        centerRepo.save(center);
//
//        return "Dispatched " + pending.size() + " items ("
//                + totalKg + " kg) to '" + processor.getName() + "'";
//    }

    public String dispatchToProcessor(Long centerId) {

        CollectionCentres center = getCenter(centerId);

        List<FoodWasteItems> approvedItems = itemRepo
                .findByCollectionCentre_IdAndAcceptedTrueAndRejectedFalseAndDispatchedFalse(centerId);

        if (approvedItems.isEmpty())
            return "No accepted items ready to dispatch at '" + center.getLocation() + "'";

        double totalKg = approvedItems.stream()
                .mapToDouble(FoodWasteItems::getWeightKg).sum();

        Processors processor = loadBalancer.findBestProcessor(totalKg);

        approvedItems.forEach(item -> item.setDispatched(true));
        itemRepo.saveAll(approvedItems);

        processor.setCurrentLoadKg(
                processor.getCurrentLoadKg() + totalKg);
        processorRepo.save(processor);

        center.setCurrentLoadKg(0.0);
        centerRepo.save(center);

        return "Dispatched " + approvedItems.size()
                + " accepted items (" + totalKg + " kg)"
                + " to '" + processor.getName() + "'";
    }

        public String dispatchSingleItem(Long centerId, Long itemId) {
                CollectionCentres center = getCenter(centerId);
                FoodWasteItems item = itemRepo.findById(itemId)
                                .orElseThrow(() -> new ResourceNotFoundException("Food waste item not found: " + itemId));

                if (item.getCollectionCentre() == null
                                || !item.getCollectionCentre().getId().equals(centerId)
                                || !item.isAccepted() || item.isRejected() || item.isDispatched()) {
                        throw new ValidationException("Only accepted, undispatched items from this center can be dispatched.");
                }

                Processors processor = loadBalancer.findBestProcessor(item.getWeightKg());
                item.setDispatched(true);
                itemRepo.save(item);
                processor.setCurrentLoadKg(processor.getCurrentLoadKg() + item.getWeightKg());
                processorRepo.save(processor);

                return "Dispatched item " + item.getId() + " (" + item.getWeightKg() + " kg)"
                                + " to '" + processor.getName() + "'";
        }

    private void mapFields(CollectionCentres c,
                           CollectionCenterRequest req) {
        c.setName(req.getName());
        c.setLocation(req.getLocation());
        c.setMaxCapicityKg(req.getMaxCapacityKg());

        if (req.getProcessorId() != null) {
            c.setProcessor(processorRepo.findById(req.getProcessorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Processor not found: "
                                    + req.getProcessorId())));
        }
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
