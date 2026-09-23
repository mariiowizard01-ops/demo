package com.fwn.foodwaste.controller;

import com.fwn.foodwaste.entity.enums.WasteType;
import com.fwn.foodwaste.service.WasteAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

        import java.util.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class ReportController {

    private final WasteAnalysisService analysisService;

    @GetMapping("/waste-type-frequency")
    public ResponseEntity<Map<WasteType, Long>> frequency() {
        return ResponseEntity.ok(
                analysisService.getWasteTypeFrequency());
    }

    @GetMapping("/waste-type-weight")
    public ResponseEntity<Map<WasteType, Double>> weight() {
        return ResponseEntity.ok(
                analysisService.getWasteTypeWeightSummary());
    }

    @GetMapping("/top-donors")
    public ResponseEntity<List<Map<String, Object>>> topDonors(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(
                analysisService.getTopDonors(limit));
    }

    @GetMapping("/full")
    public ResponseEntity<Map<String, Object>> fullReport() {
        return ResponseEntity.ok(
                analysisService.getFullReport());
    }
}
