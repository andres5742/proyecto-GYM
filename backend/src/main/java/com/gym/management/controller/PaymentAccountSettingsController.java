package com.gym.management.controller;

import com.gym.management.dto.PaymentAccountSettingsResponse;
import com.gym.management.dto.UpdatePaymentAccountSettingsRequest;
import com.gym.management.model.PaymentAccountSettings;
import com.gym.management.exception.BusinessException;
import com.gym.management.service.PaymentAccountBalanceService;
import com.gym.management.util.MoneyUtil;
import com.gym.management.util.TreasuryAccess;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/payment-account-settings")
@RequiredArgsConstructor
public class PaymentAccountSettingsController {

    private final PaymentAccountBalanceService paymentAccountBalanceService;

    @GetMapping
    public PaymentAccountSettingsResponse get() {
        requireTreasuryAccess();
        return toResponse(paymentAccountBalanceService);
    }

    @PutMapping
    public PaymentAccountSettingsResponse update(@Valid @RequestBody UpdatePaymentAccountSettingsRequest request) {
        requireTreasuryAccess();
        paymentAccountBalanceService.saveSettings(
                request.nequiInitialBalance(), request.bancolombiaInitialBalance());
        return toResponse(paymentAccountBalanceService);
    }

    private static void requireTreasuryAccess() {
        if (!TreasuryAccess.canViewTreasuryBalances()) {
            throw new BusinessException("Solo administración puede ver o configurar saldos de Nequi y Bancolombia");
        }
    }

    private static PaymentAccountSettingsResponse toResponse(PaymentAccountBalanceService balanceService) {
        PaymentAccountSettings settings = balanceService.getSettings();
        return new PaymentAccountSettingsResponse(
                MoneyUtil.roundPesos(settings.getNequiInitialBalance()),
                MoneyUtil.roundPesos(settings.getBancolombiaInitialBalance()),
                balanceService.computeCurrentMonthBalances());
    }
}
