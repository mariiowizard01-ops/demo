package com.fwn.foodwaste.controller;

import com.fwn.foodwaste.dto.Request.ProcessorRequest;
import com.fwn.foodwaste.dto.Response.ProcessorResponse;
import com.fwn.foodwaste.service.ProcessorLoadBalancerService;
import com.fwn.foodwaste.service.ProcessorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/processors")
@RequiredArgsConstructor
public class ProcessorController {

    private final ProcessorLoadBalancerService loadBalancer;
    private final ProcessorService processorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<List<ProcessorResponse>> getAll() {
        return ResponseEntity.ok(processorService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ProcessorResponse> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(processorService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ProcessorResponse> create(
            @Valid @RequestBody ProcessorRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(processorService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ProcessorResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProcessorRequest request) {
        return ResponseEntity.ok(processorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        processorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // this is controller to balance the load to processors
    @GetMapping("/load-summary")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Map<String, Double>> loadSummary() {
        return ResponseEntity.ok(loadBalancer.getLoadSummary());
    }
}
