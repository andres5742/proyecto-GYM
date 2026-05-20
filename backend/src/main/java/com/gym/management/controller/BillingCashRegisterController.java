package com.gym.management.controller;

import com.gym.management.dto.BillingCashRegisterExpenseRequest;
import com.gym.management.dto.BillingCashRegisterExpenseResponse;
import com.gym.management.dto.BillingCashRegisterResponse;
import com.gym.management.dto.OpenBillingCashRegisterRequest;
import com.gym.management.service.BillingCashRegisterService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/cash-registers")
@RequiredArgsConstructor
public class BillingCashRegisterController {

    private final BillingCashRegisterService cashRegisterService;

    @GetMapping("/open")
    public ResponseEntity<BillingCashRegisterResponse> findOpen() {
        BillingCashRegisterResponse reg = cashRegisterService.findOpen();
        if (reg == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(reg);
    }

    @GetMapping("/today")
    public ResponseEntity<BillingCashRegisterResponse> findToday() {
        BillingCashRegisterResponse reg = cashRegisterService.findToday();
        if (reg == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(reg);
    }

    @GetMapping
    public List<BillingCashRegisterResponse> findByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return cashRegisterService.findByDate(date);
    }

    @PostMapping("/open")
    public BillingCashRegisterResponse open(@Valid @RequestBody OpenBillingCashRegisterRequest request) {
        return cashRegisterService.open(request);
    }

    @PostMapping("/{id}/close")
    public BillingCashRegisterResponse close(@PathVariable Long id) {
        return cashRegisterService.close(id);
    }

    @GetMapping("/expenses")
    public List<BillingCashRegisterExpenseResponse> listExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return cashRegisterService.listExpensesByDate(date);
    }

    @PostMapping("/expenses")
    public BillingCashRegisterExpenseResponse addExpense(
            @Valid @RequestBody BillingCashRegisterExpenseRequest request) {
        return cashRegisterService.addExpense(request);
    }
}
