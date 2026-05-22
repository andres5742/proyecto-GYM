package com.gym.management.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gym.management.model.BillingPaymentType;
import com.gym.management.model.MembershipPaymentKind;
import com.gym.management.model.PaymentMethod;
import com.gym.management.util.PesosJsonSerializer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BillingPaymentResponse(
        Long id,
        BillingPaymentType paymentType,
        String paymentTypeLabel,
        Long memberId,
        String memberName,
        Long planId,
        String planName,
        Long saleId,
        PaymentMethod paymentMethod,
        String paymentMethodLabel,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal amount,
        LocalDate paymentDate,
        LocalDate membershipStart,
        LocalDate membershipEnd,
        MembershipPaymentKind membershipPaymentKind,
        String membershipPaymentKindLabel,
        Long recordedByEmployeeId,
        String recordedByEmployeeName,
        Instant createdAt) {}
