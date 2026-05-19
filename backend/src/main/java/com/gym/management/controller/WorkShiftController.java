package com.gym.management.controller;

import com.gym.management.dto.SalesSummaryResponse;
import com.gym.management.dto.WorkShiftRequest;
import com.gym.management.dto.WorkShiftResponse;
import com.gym.management.service.SaleService;
import com.gym.management.service.WorkShiftService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class WorkShiftController {

    private final WorkShiftService workShiftService;
    private final SaleService saleService;

    @GetMapping
    public List<WorkShiftResponse> findAll() {
        return workShiftService.findAll();
    }

    @GetMapping("/open")
    public ResponseEntity<WorkShiftResponse> findOpen() {
        WorkShiftResponse open = workShiftService.findOpen();
        if (open == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(open);
    }

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkShiftResponse open(@Valid @RequestBody WorkShiftRequest request) {
        return workShiftService.open(request);
    }

    @PostMapping("/{id}/close")
    public WorkShiftResponse close(@PathVariable Long id) {
        return workShiftService.close(id);
    }

    @GetMapping("/{id}/sales-summary")
    public SalesSummaryResponse salesSummary(@PathVariable Long id) {
        return saleService.getSummary(id);
    }
}
