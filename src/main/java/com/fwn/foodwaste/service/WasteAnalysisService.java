package com.fwn.foodwaste.service;

import com.fwn.foodwaste.entity.FoodWasteItems;
import com.fwn.foodwaste.entity.enums.WasteType;
import com.fwn.foodwaste.repository.FoodWasteItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
        import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WasteAnalysisService {

    private final FoodWasteItemRepository itemRepo;

    /**
     * HASHMAP FREQUENCY ANALYSIS
     * Counts items per waste type in a single O(n) pass.
     * Map.merge() increments count or initialises to 1.
     * Result sorted by count descending.
     */
    @Transactional(readOnly = true)
    public Map<WasteType, Long> getWasteTypeFrequency() {

        List<FoodWasteItems> all = itemRepo.findAll();

        Map<WasteType, Long> frequency = new HashMap<>();

        for (FoodWasteItems item : all) {
            frequency.merge(item.getWasteType(), 1L, Long::sum);
        }

        return frequency.entrySet().stream()
                .sorted(Map.Entry.<WasteType, Long>
                        comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    /**
     * Total kg accumulated per waste type.
     * Same HashMap pattern — accumulates Double instead of Long.
     */
    @Transactional(readOnly = true)
    public Map<WasteType, Double> getWasteTypeWeightSummary() {

        List<FoodWasteItems> all = itemRepo.findAll();

        Map<WasteType, Double> weightMap = new HashMap<>();

        for (FoodWasteItems item : all) {
            weightMap.merge(
                    item.getWasteType(),
                    item.getWeightKg(),
                    Double::sum);
        }

        return weightMap.entrySet().stream()
                .sorted(Map.Entry.<WasteType, Double>
                        comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    /**
     * SORTING ALGORITHM — Top donors by total kg donated.
     * HashMap accumulates per donor, then Comparator sorts descending.
     * Time complexity: O(n) accumulate + O(d log d) sort
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopDonors(int limit) {

        List<FoodWasteItems> all = itemRepo.findAll();

        Map<String, Double> donorTotals = new HashMap<>();

        for (FoodWasteItems item : all) {
            if (item.getDonor() == null) continue;
            donorTotals.merge(
                    item.getDonor().getName(),
                    item.getWeightKg(),
                    Double::sum);
        }

        return donorTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>
                        comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("donorName", e.getKey());
                    row.put("totalKg",   e.getValue());
                    return row;
                })
                .collect(Collectors.toList());
    }

    /**
     * Full combined report — all analysis in one call.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFullReport() {

        List<FoodWasteItems> all = itemRepo.findAll();

        long totalItems      = all.size();
        long processedItems  = all.stream()
                .filter(FoodWasteItems::isProcessed).count();
        double totalWeightKg = all.stream()
                .mapToDouble(FoodWasteItems::getWeightKg).sum();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalItems",      totalItems);
        report.put("processedItems",  processedItems);
        report.put("pendingItems",    totalItems - processedItems);
        report.put("totalWeightKg",   totalWeightKg);
        report.put("byTypeCount",     getWasteTypeFrequency());
        report.put("byTypeWeightKg",  getWasteTypeWeightSummary());
        report.put("topDonors",       getTopDonors(5));

        return report;
    }
}
