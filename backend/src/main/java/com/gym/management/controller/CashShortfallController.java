package com.gym.management.controller;

import com.gym.management.dto.CashShortfallMonthlySummaryResponse;
import com.gym.management.dto.CashShortfallResponse;
import com.gym.management.dto.SettleCashShortfallRequest;
import com.gym.management.service.CashShortfallService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cash-shortfalls")
@RequiredArgsConstructor
public class CashShortfallController {

    private final CashShortfallService cashShortfallService;

    @GetMapping
    public List<CashShortfallResponse> findForMonth(
            @RequestParam int year, @RequestParam int month) {
        return cashShortfallService.findForMonth(year, month);
    }

    @GetMapping("/summary")
    public List<CashShortfallMonthlySummaryResponse> monthlySummary(
            @RequestParam int year, @RequestParam int month) {
        return cashShortfallService.monthlySummary(year, month);
    }

    @PatchMapping("/{id}/settle")
    public CashShortfallResponse settle(
            @PathVariable Long id, @Valid @RequestBody(required = false) SettleCashShortfallRequest request) {
        return cashShortfallService.settle(id, request);
    }
}
