package com.fwn.foodwaste.controller;


import com.fwn.foodwaste.dto.Request.AutoAssignFoodWasteItemRequest;
import com.fwn.foodwaste.dto.Request.FoodWasteItemRequest;
import com.fwn.foodwaste.dto.Response.FoodWasteItemResponse;
import com.fwn.foodwaste.service.FefoProcessingService;
import com.fwn.foodwaste.service.FoodWasteItemService;
import com.fwn.foodwaste.service.GreedyCollectionCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/food-waste-items")
@RequiredArgsConstructor
public class FoodWasteItemController {

    private final FefoProcessingService fefoService;
    private final GreedyCollectionCenterService greedyService;
    private final FoodWasteItemService itemService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','DONOR')")
    public ResponseEntity<List<FoodWasteItemResponse>> getAll() {
        return ResponseEntity.ok(itemService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','DONOR')")
    public ResponseEntity<FoodWasteItemResponse> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(itemService.findById(id));
    }

    @GetMapping("/by-donor/{donorId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','DONOR')")
    public ResponseEntity<List<FoodWasteItemResponse>> getByDonor(
            @PathVariable Long donorId) {
        return ResponseEntity.ok(itemService.findByDonor(donorId));
    }

    @GetMapping("/by-center/{centerId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<List<FoodWasteItemResponse>> getByCenter(
            @PathVariable Long centerId) {
        return ResponseEntity.ok(itemService.findByCenter(centerId));
    }

    @GetMapping("/processing-queue")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<List<FoodWasteItemResponse>> getQueue() {
        return ResponseEntity.ok(itemService.getProcessingQueue());
    }

    @PostMapping
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<FoodWasteItemResponse> create(
            @Valid @RequestBody FoodWasteItemRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(itemService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','DONOR')")
    public ResponseEntity<FoodWasteItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FoodWasteItemRequest request) {
        return ResponseEntity.ok(itemService.update(id, request));
    }

    @PatchMapping("/{id}/accept")
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<FoodWasteItemResponse> acceptWaste(
            @PathVariable Long id) {
        return ResponseEntity.ok(itemService.accept(id));
    }

    @PatchMapping("/{id}/reject")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<FoodWasteItemResponse> rejectWaste(
            @PathVariable Long id) {
        return ResponseEntity.ok(itemService.reject(id));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<FoodWasteItemResponse> completeProcessing(
            @PathVariable Long id) {
        return ResponseEntity.ok(itemService.completeProcessing(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Greedy auto-assign (no centerId needed in body)
    @PostMapping("/auto-assign")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<FoodWasteItemResponse> createWithAutoAssign(
            @Valid @RequestBody AutoAssignFoodWasteItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemService.createWithAutoAssign(req));
    }

    // FEFO full queue
    @GetMapping("/fefo-queue")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<List<FoodWasteItemResponse>> fefoQueue() {
        return ResponseEntity.ok(fefoService.getFefoQueue());
    }

    // FEFO for one center
    @GetMapping("/fefo-queue/center/{centerId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<List<FoodWasteItemResponse>> fefoQueueForCenter(
            @PathVariable Long centerId) {
        return ResponseEntity.ok(
                fefoService.getFefoQueueForCenter(centerId));
    }

    // Urgency based on the date of food items
    @GetMapping("/urgency")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Map<String, Object>> urgency() {
        return ResponseEntity.ok(fefoService.getUrgencyBands());
    }

}