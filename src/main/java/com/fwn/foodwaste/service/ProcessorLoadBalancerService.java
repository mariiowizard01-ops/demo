package com.fwn.foodwaste.service;


import com.fwn.foodwaste.entity.Processors;
import com.fwn.foodwaste.exception.CapacityExceededException;
import com.fwn.foodwaste.repository.ProcessorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessorLoadBalancerService {

    private final ProcessorRepository processorRepo;

    /**
     * LOAD BALANCING ALGORITHM
     * Among processors that can fit the batch,
     * picks the one with the MOST free capacity.
     * Prevents any single processor from becoming a bottleneck.
     * Time complexity: O(n)
     */
    @Transactional(readOnly = true)
    public Processors findBestProcessor(double batchKg) {

        List<Processors> all = processorRepo.findAll();

        return all.stream()
                // only processors that can fit the batch
                .filter(p -> p.getFreeCapacity() >= batchKg)
                // pick the one with most free capacity
                .max(Comparator.comparingDouble(Processors::getFreeCapacity))
                .orElseThrow(() -> new CapacityExceededException(
                        "No processor can accept " + batchKg
                                + " kg. All processors are at capacity."));
    }

    /**
     * Returns load percentage for every processor.
     * Least loaded processor appears first.
     */
    @Transactional(readOnly = true)
    public Map<String, Double> getLoadSummary() {

        List<Processors> all = processorRepo.findAll();

        Map<String, Double> summary = new LinkedHashMap<>();

        all.stream()
                .sorted(Comparator.comparingDouble(p ->
                        p.getCurrentLoadKg()
                                / p.getMaxProcessingCapicityKg()))
                .forEach(p -> {
                    double pct = (p.getCurrentLoadKg()
                            / p.getMaxProcessingCapicityKg()) * 100.0;
                    summary.put(p.getName(),
                            Math.round(pct * 10.0) / 10.0);
                });

        return summary;
    }
}
