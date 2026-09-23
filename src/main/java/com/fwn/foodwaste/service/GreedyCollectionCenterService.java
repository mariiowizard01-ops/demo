package com.fwn.foodwaste.service;

import com.fwn.foodwaste.dto.Response.CollectionCenterResponse;
import com.fwn.foodwaste.entity.CollectionCentres;
import com.fwn.foodwaste.exception.CapacityExceededException;
import com.fwn.foodwaste.repository.CollectionCenterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GreedyCollectionCenterService {

    private final CollectionCenterRepository centerRepo;
    private final CollectionCenterService centreService;

    /**
     * GREEDY ALGORITHM
     * Among all centers that can fit the weight,
     * picks the one with the MOST free space.
     * Spreads load evenly across all centers.
     *
     * Time complexity: O(n)
     */
    @Transactional(readOnly = true)
    public CollectionCentres findBestCenter(double weightKg) {

        List<CollectionCentres> all = centerRepo.findAll();

        return all.stream()
                // only centers that physically fit the item
                .filter(c -> c.hasCapacity(weightKg))
                // greedy pick — most free space wins
                .max(Comparator.comparingDouble(
                        c -> c.getMaxCapicityKg() - c.getCurrentLoadKg()))
                .orElseThrow(() -> new CapacityExceededException(
                        "No collection center has enough capacity "
                                + "to accept " + weightKg + " kg."));
    }

    /**
     * Returns all centers ranked by free space descending.
     * Used by the dashboard to show which centers still have room.
     */
//    @Transactional(readOnly = true)
//    public List<CollectionCentres> getRankedCenters() {
//
//        List<CollectionCentres> all = centerRepo.findAll();
//
//        all.sort(Comparator.comparingDouble(
//                        (CollectionCentres c) ->
//                                c.getMaxCapicityKg() - c.getCurrentLoadKg())
//                .reversed());
//
//        return all;
//    }
    @Transactional(readOnly = true)
    public List<CollectionCenterResponse> getRankedCenters() {
        List<CollectionCentres> all = centerRepo.findAll();

        all.sort(Comparator.comparingDouble(
                        (CollectionCentres c) ->
                                c.getMaxCapicityKg() - c.getCurrentLoadKg())
                .reversed());

        return all.stream().map(centreService::toResponse).toList();
    }


}
