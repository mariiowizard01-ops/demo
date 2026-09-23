package com.fwn.foodwaste.repository;

import com.fwn.foodwaste.entity.CollectionCentres;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CollectionCenterRepository extends JpaRepository<CollectionCentres, Long> {
    List<CollectionCentres> findByProcessorId(Long processorId);

    // centers where load exceeds threshold — useful for capacity warnings
//    @Query("SELECT c FROM CollectionCenter c " +
//            "WHERE (c.currentLoadKg / c.maxCapacityKg) >= :threshold")
//    List<CollectionCentres> findCentersAboveCapacityThreshold(
//            double threshold);
}
