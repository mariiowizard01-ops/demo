package com.fwn.foodwaste.repository;

import com.fwn.foodwaste.entity.FoodWasteItems;
import com.fwn.foodwaste.entity.enums.WasteType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodWasteItemRepository extends JpaRepository<FoodWasteItems, Long> {

    List<FoodWasteItems> findByDonorId(Long donorId);

    List<FoodWasteItems> findByCollectionCentre_Id(Long centerId);

    List<FoodWasteItems> findByCollectionCentre_IdAndAcceptedTrueAndRejectedFalseAndDispatchedFalse(Long centerId);

    List<FoodWasteItems> findByAcceptedTrueAndRejectedFalseAndDispatchedFalseOrderByExpirationDateAsc();

    List<FoodWasteItems> findByCollectionCentre_IdAndAcceptedTrueAndRejectedFalseAndDispatchedFalseOrderByExpirationDateAsc(Long centerId);

    List<FoodWasteItems> findByCollectionCentre_IdAndProcessedFalse(Long centerId);

    List<FoodWasteItems> findByProcessedFalseOrderByExpirationDateAsc();

    List<FoodWasteItems> findByWasteType(WasteType wasteType);

    List<FoodWasteItems> findByProcessedFalse();

}
