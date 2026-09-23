package com.fwn.foodwaste.controller;

import com.fwn.foodwaste.dto.Request.FoodDonorRequest;
import com.fwn.foodwaste.dto.Response.FoodDonorResponse;
import com.fwn.foodwaste.service.FoodDonorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food-donors")
@RequiredArgsConstructor
public class FoodDonorController {

    private final FoodDonorService donorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','DONOR')")
    public ResponseEntity<List<FoodDonorResponse>> getAll() {
        return ResponseEntity.ok(donorService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','DONOR')")
    public ResponseEntity<FoodDonorResponse> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(donorService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<FoodDonorResponse> create(
            @Valid @RequestBody FoodDonorRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(donorService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','DONOR')")
    public ResponseEntity<FoodDonorResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FoodDonorRequest request) {
        return ResponseEntity.ok(donorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        donorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
