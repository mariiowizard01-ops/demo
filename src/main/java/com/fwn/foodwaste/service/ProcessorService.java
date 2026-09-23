package com.fwn.foodwaste.service;

import com.fwn.foodwaste.dto.Request.ProcessorRequest;
import com.fwn.foodwaste.dto.Response.ProcessorResponse;
import com.fwn.foodwaste.entity.Processors;
import com.fwn.foodwaste.exception.CapicityExceedException;
import com.fwn.foodwaste.exception.ResourceNotFoundException;
import com.fwn.foodwaste.repository.ProcessorRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcessorService {


    private final ProcessorRepository processorRepo;

    @Transactional(readOnly = true)
    public List<ProcessorResponse> findAll() {
        return processorRepo.findAll()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProcessorResponse findById(Long id) {
        return toResponse(getProcessor(id));
    }

    public ProcessorResponse create(ProcessorRequest req) {
        Processors p = new Processors();
        mapFields(p, req);
        return toResponse(processorRepo.save(p));
    }

    public ProcessorResponse update(Long id, ProcessorRequest req) {
        Processors p = getProcessor(id);
        mapFields(p, req);
        return toResponse(processorRepo.save(p));
    }

    public void delete(Long id) {
        if (!processorRepo.existsById(id))
            throw new ResourceNotFoundException("Processor not found: " + id);
        processorRepo.deleteById(id);
    }

    // Used by GreedyAllocationService
//    public Processors findBestProcessor(double batchKg) {
//        return processorRepo.findAllOrderedByFreeCapacity()
//                .stream()
//                .filter(p -> p.getFreeCapacity() >= batchKg)
//                .findFirst()
//                .orElseThrow(() -> new CapicityExceedException(
//                        "No processor has enough capacity for "
//                                + batchKg + " kg"));
//    }

    private void mapFields(Processors p, ProcessorRequest req) {
        p.setName(req.getName());
        p.setLocation(req.getLocation());
        p.setMaxProcessingCapicityKg(req.getMaxProcessingCapacityKg());
    }

    public Processors getProcessor(Long id) {
        return processorRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Processor not found: " + id));
    }

    private ProcessorResponse toResponse(Processors p) {
        return ProcessorResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .location(p.getLocation())
                .maxProcessingCapacityKg(p.getMaxProcessingCapicityKg())
                .currentLoadKg(p.getCurrentLoadKg())
                .freeCapacityKg(p.getFreeCapacity())
                .assignedCentersCount(p.getCollectionCentres().size())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
