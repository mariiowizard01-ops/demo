package com.fwn.foodwaste.controller;


import com.fwn.foodwaste.dto.Request.CollectionCenterRequest;
import com.fwn.foodwaste.dto.Response.CollectionCenterResponse;
import com.fwn.foodwaste.entity.CollectionCentres;
import com.fwn.foodwaste.service.CollectionCenterService;
import com.fwn.foodwaste.service.GreedyCollectionCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collection-centers")
@RequiredArgsConstructor
public class CollectionCenterController {

    private final GreedyCollectionCenterService greedyService;
    private final CollectionCenterService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','DONOR')")
    public ResponseEntity<List<CollectionCenterResponse>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CollectionCenterResponse> create(
            @Valid @RequestBody CollectionCenterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CollectionCenterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CollectionCenterRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Map<String, String>> dispatch(@PathVariable Long id) {
        String result = service.dispatchToProcessor(id);
        return ResponseEntity.ok(Map.of("message", result));
    }

    @PostMapping("/{centerId}/dispatch/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Map<String, String>> dispatchItem(
            @PathVariable Long centerId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(Map.of("message", service.dispatchSingleItem(centerId, itemId)));
    }

    // use to show which collection center is in best form
    @GetMapping("/ranked")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<List<CollectionCenterResponse>> ranked() {
        return ResponseEntity.ok(greedyService.getRankedCenters());
    }

//    public ResponseEntity<List<CollectionCenterResponse>> ranked() {
//        return ResponseEntity.ok(
//                greedyService.getRankedCenters()
//                        .stream()
//                        .map(this::toResponse)
//                        .toList()
//        );
//    }



}
