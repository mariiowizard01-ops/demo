package com.fwn.foodwaste.service;


import com.fwn.foodwaste.dto.Response.FoodWasteItemResponse;
import com.fwn.foodwaste.entity.FoodWasteItems;
import com.fwn.foodwaste.repository.FoodWasteItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FefoProcessingService {

    private final FoodWasteItemRepository itemRepo;
    private final FoodWasteItemService itemService;

    /**
     * FEFO — First Expired First Out
     * Uses a Min-Heap PriorityQueue.
     * FoodWasteItems.compareTo() orders by expirationDate ascending
     * so soonest expiry item is always at the top of the heap.
     *
     * Time complexity: O(n log n)
     */
    @Transactional(readOnly = true)
    public List<FoodWasteItemResponse> getFefoQueue() {

        List<FoodWasteItems> unprocessed =
            itemRepo.findByAcceptedTrueAndRejectedFalseAndDispatchedFalseOrderByExpirationDateAsc();

        if (unprocessed.isEmpty())
            return Collections.emptyList();

        // Build min-heap on expirationDate
        PriorityQueue<FoodWasteItems> minHeap =
                new PriorityQueue<>(unprocessed);

        // Drain heap — soonest expiry comes out first
        List<FoodWasteItemResponse> ordered = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            ordered.add(itemService.toResponse(minHeap.poll()));
        }

        return ordered;
    }


     //FEFO for a specific center only.

    @Transactional(readOnly = true)
    public List<FoodWasteItemResponse> getFefoQueueForCenter(Long centerId) {

        List<FoodWasteItems> pending =
            itemRepo.findByCollectionCentre_IdAndAcceptedTrueAndRejectedFalseAndDispatchedFalseOrderByExpirationDateAsc(centerId);

        if (pending.isEmpty())
            return Collections.emptyList();

        PriorityQueue<FoodWasteItems> minHeap =
                new PriorityQueue<>(pending);

        List<FoodWasteItemResponse> ordered = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            ordered.add(itemService.toResponse(minHeap.poll()));
        }

        return ordered;
    }

    /**
     * Splits all unprocessed items into 3 urgency bands.
     * CRITICAL → 0–3 days until expiry
     * WARNING  → 4–7 days until expiry
     * OK       → 8+ days until expiry
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUrgencyBands() {

        List<FoodWasteItems> all =
                itemRepo.findByProcessedFalse();

        List<FoodWasteItemResponse> critical = new ArrayList<>();
        List<FoodWasteItemResponse> warning  = new ArrayList<>();
        List<FoodWasteItemResponse> ok       = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (FoodWasteItems item : all) {
            long daysLeft = ChronoUnit.DAYS.between(
                    today, item.getExpirationDate());
            FoodWasteItemResponse dto = itemService.toResponse(item);
            if (daysLeft <= 3)      critical.add(dto);
            else if (daysLeft <= 7) warning.add(dto);
            else                    ok.add(dto);
        }

        Comparator<FoodWasteItemResponse> byExpiry =
                Comparator.comparing(FoodWasteItemResponse::getExpirationDate);

        critical.sort(byExpiry);
        warning.sort(byExpiry);
        ok.sort(byExpiry);

        Map<String, Object> bands = new LinkedHashMap<>();
        bands.put("CRITICAL", critical);
        bands.put("WARNING",  warning);
        bands.put("OK",       ok);
        bands.put("summary",  Map.of(
                "criticalCount", critical.size(),
                "warningCount",  warning.size(),
                "okCount",       ok.size()
        ));

        return bands;
    }
}
