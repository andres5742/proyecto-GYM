package com.gym.management.controller;

import com.gym.management.dto.PayrollConfigRequest;
import com.gym.management.dto.PayrollConfigResponse;
import com.gym.management.service.PayrollConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payroll-config")
@RequiredArgsConstructor
public class PayrollConfigController {

    private final PayrollConfigService payrollConfigService;

    @GetMapping
    public PayrollConfigResponse get() {
        return payrollConfigService.get();
    }

    @PutMapping
    public PayrollConfigResponse update(@Valid @RequestBody PayrollConfigRequest request) {
        return payrollConfigService.update(request);
    }
}
