package com.gym.management.controller;

import com.gym.management.dto.SaleRequest;
import com.gym.management.dto.SaleResponse;
import com.gym.management.dto.SalesSummaryResponse;
import com.gym.management.service.SaleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public List<SaleResponse> findAll(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long workShiftId) {
        return saleService.findAll(employeeId, workShiftId);
    }

    @GetMapping("/summary")
    public SalesSummaryResponse getSummary(@RequestParam(required = false) Long workShiftId) {
        return saleService.getSummary(workShiftId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse create(@Valid @RequestBody SaleRequest request) {
        return saleService.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        saleService.delete(id);
    }
}
