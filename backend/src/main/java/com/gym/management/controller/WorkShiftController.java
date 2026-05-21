package com.gym.management.controller;

import com.gym.management.dto.SalesSummaryResponse;
import com.gym.management.dto.ShiftDetailResponse;
import com.gym.management.dto.ShiftOpenInventoryPreviewResponse;
import com.gym.management.dto.WorkShiftOpenResultResponse;
import com.gym.management.dto.WorkShiftRequest;
import com.gym.management.dto.WorkShiftResponse;
import com.gym.management.service.SaleService;
import com.gym.management.service.WorkShiftService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/open-inventory-preview")
    public ShiftOpenInventoryPreviewResponse openInventoryPreview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shiftDate) {
        return workShiftService.openInventoryPreview(shiftDate);
    }

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkShiftOpenResultResponse open(@Valid @RequestBody WorkShiftRequest request) {
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

    @GetMapping("/{id}/detail")
    public ShiftDetailResponse detail(@PathVariable Long id) {
        return saleService.getShiftDetail(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        workShiftService.delete(id);
    }
}
