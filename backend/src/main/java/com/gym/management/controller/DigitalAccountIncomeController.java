package com.gym.management.controller;

import com.gym.management.dto.DigitalAccountIncomeLineResponse;
import com.gym.management.dto.DigitalAccountIncomeSource;
import com.gym.management.service.DigitalAccountIncomeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/digital-account-incomes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class DigitalAccountIncomeController {

    private final DigitalAccountIncomeService digitalAccountIncomeService;

    @GetMapping
    public List<DigitalAccountIncomeLineResponse> listCurrentMonth() {
        return digitalAccountIncomeService.listCurrentMonth();
    }

    @DeleteMapping("/{source}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable DigitalAccountIncomeSource source, @PathVariable Long id) {
        digitalAccountIncomeService.delete(source, id);
    }
}
