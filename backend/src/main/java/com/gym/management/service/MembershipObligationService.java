package com.gym.management.service;

import com.gym.management.dto.MembershipObligationResponse;
import com.gym.management.dto.MembershipPaymentOutcomeResponse;
import com.gym.management.dto.PaymentSplitLine;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.MembershipObligationMapper;
import com.gym.management.model.BillingCashRegister;
import com.gym.management.model.BillingPayment;
import com.gym.management.model.BillingPaymentType;
import com.gym.management.model.Employee;
import com.gym.management.model.Member;
import com.gym.management.model.MembershipObligation;
import com.gym.management.model.MembershipObligationStatus;
import com.gym.management.model.MembershipPaymentKind;
import com.gym.management.model.MembershipPlan;
import com.gym.management.model.MembershipStatus;
import com.gym.management.model.PaymentMethod;
import com.gym.management.repository.BillingPaymentRepository;
import com.gym.management.repository.MemberRepository;
import com.gym.management.repository.MembershipObligationRepository;
import com.gym.management.util.MoneyUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipObligationService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private final MembershipObligationRepository obligationRepository;
    private final MemberRepository memberRepository;
    private final BillingPaymentRepository billingPaymentRepository;

    @Transactional(readOnly = true)
    public Optional<MembershipObligationResponse> findOpenForMember(Long memberId) {
        return obligationRepository
                .findOpenByMemberIdWithDetails(memberId, MembershipObligationStatus.OPEN)
                .map(MembershipObligationMapper::toResponse);
    }

    @Transactional
    public MembershipPaymentOutcomeResponse registerPayment(
            Member member,
            MembershipPlan plan,
            int monthsPaid,
            PaymentMethod paymentMethod,
            Long amountPesos,
            List<PaymentSplitLine> paymentSplits,
            BillingCashRegister cashRegister,
            Employee employee,
            Long existingObligationId) {

        List<PaymentSplitLine> splits = resolvePaymentSplits(paymentMethod, amountPesos, paymentSplits);
        PaymentMethod primaryMethod = splits.get(0).paymentMethod();
        BillingPaymentMethodRules.requireAllowed(primaryMethod);
        BigDecimal paymentAmount = splits.stream()
                .map(line -> requirePositivePesos(line.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (existingObligationId != null) {
            return applyPaymentToObligation(
                    existingObligationId, member.getId(), paymentAmount, splits, cashRegister, employee);
        }

        if (obligationRepository.existsByMemberIdAndStatus(member.getId(), MembershipObligationStatus.OPEN)) {
            MembershipObligation open = obligationRepository
                    .findOpenByMemberIdWithDetails(member.getId(), MembershipObligationStatus.OPEN)
                    .orElseThrow();
            BigDecimal balance = MembershipObligationMapper.balanceOf(open);
            throw new BusinessException(
                    "Este afiliado tiene un saldo pendiente de $"
                            + MoneyUtil.formatPesos(balance)
                            + " por la membresía «"
                            + open.getPlan().getName()
                            + "». Registre un abono antes de crear otra obligación.");
        }

        BigDecimal total = MoneyUtil.roundPesos(plan.getPrice().multiply(BigDecimal.valueOf(monthsPaid)));
        requireSplitAllowedForFullPayment(paymentSplits, paymentAmount, total, existingObligationId);
        if (paymentAmount.compareTo(total) > 0) {
            throw new BusinessException(
                    "El abono ($"
                            + MoneyUtil.formatPesos(paymentAmount)
                            + ") supera el total de la membresía ($"
                            + MoneyUtil.formatPesos(total)
                            + ")");
        }

        MembershipDates dates = computeMembershipDates(member, plan, monthsPaid);
        MembershipPaymentKind kind =
                paymentAmount.compareTo(total) >= 0 ? MembershipPaymentKind.FULL : MembershipPaymentKind.PARTIAL;

        MembershipObligation obligation = MembershipObligation.builder()
                .member(member)
                .plan(plan)
                .monthsPaid(monthsPaid)
                .totalAmount(total)
                .amountPaid(paymentAmount)
                .status(kind == MembershipPaymentKind.FULL ? MembershipObligationStatus.PAID : MembershipObligationStatus.OPEN)
                .plannedMembershipStart(dates.start())
                .plannedMembershipEnd(dates.end())
                .build();
        obligation = obligationRepository.save(obligation);

        applyMembershipToMember(member, plan, dates, MembershipObligationMapper.balanceOf(obligation));
        memberRepository.save(member);

        BillingPayment billing = saveMembershipBillingPayments(
                member, plan, monthsPaid, splits, cashRegister, employee, obligation, kind, dates);

        return buildOutcome(billing, obligation);
    }

    private MembershipPaymentOutcomeResponse applyPaymentToObligation(
            Long obligationId,
            Long memberId,
            BigDecimal paymentAmount,
            List<PaymentSplitLine> splits,
            BillingCashRegister cashRegister,
            Employee employee) {

        MembershipObligation obligation = obligationRepository
                .findByIdWithDetails(obligationId)
                .orElseThrow(() -> new ResourceNotFoundException("Obligación de membresía no encontrada: " + obligationId));

        if (obligation.getStatus() != MembershipObligationStatus.OPEN) {
            throw new BusinessException("Esta membresía ya está pagada en su totalidad");
        }
        if (!obligation.getMember().getId().equals(memberId)) {
            throw new BusinessException("La obligación no corresponde a este afiliado");
        }

        BigDecimal balance = MembershipObligationMapper.balanceOf(obligation);
        if (paymentAmount.compareTo(balance) > 0) {
            throw new BusinessException(
                    "El abono ($"
                            + MoneyUtil.formatPesos(paymentAmount)
                            + ") supera el saldo pendiente ($"
                            + MoneyUtil.formatPesos(balance)
                            + ")");
        }

        Member member = obligation.getMember();
        MembershipPlan plan = obligation.getPlan();
        MembershipDates dates = new MembershipDates(
                obligation.getPlannedMembershipStart(), obligation.getPlannedMembershipEnd());

        BigDecimal newPaid = MoneyUtil.roundPesos(obligation.getAmountPaid().add(paymentAmount));
        obligation.setAmountPaid(newPaid);
        BigDecimal newBalance = MembershipObligationMapper.balanceOf(obligation);

        MembershipPaymentKind kind =
                newBalance.compareTo(BigDecimal.ZERO) <= 0 ? MembershipPaymentKind.FULL : MembershipPaymentKind.PARTIAL;

        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            obligation.setStatus(MembershipObligationStatus.PAID);
            obligation.setAmountPaid(obligation.getTotalAmount());
        }
        applyMembershipToMember(member, plan, dates, newBalance);
        obligationRepository.save(obligation);
        memberRepository.save(member);

        BillingPayment billing = saveMembershipBillingPayments(
                member,
                plan,
                obligation.getMonthsPaid(),
                splits,
                cashRegister,
                employee,
                obligation,
                kind,
                dates);

        return buildOutcome(billing, obligation);
    }

    @Transactional
    public void revertPayment(BillingPayment payment) {
        MembershipObligation obligation = payment.getMembershipObligation();
        if (obligation == null) {
            return;
        }
        obligation = obligationRepository
                .findByIdWithDetails(obligation.getId())
                .orElse(null);
        if (obligation == null) {
            return;
        }

        BigDecimal newPaid = MoneyUtil.roundPesos(obligation.getAmountPaid().subtract(payment.getAmount()));
        if (newPaid.compareTo(BigDecimal.ZERO) < 0) {
            newPaid = BigDecimal.ZERO;
        }
        obligation.setAmountPaid(newPaid);

        if (obligation.getStatus() == MembershipObligationStatus.PAID) {
            obligation.setStatus(MembershipObligationStatus.OPEN);
        }

        if (newPaid.compareTo(BigDecimal.ZERO) <= 0) {
            Member member = obligation.getMember();
            if (membershipMatchesObligation(member, obligation)) {
                member.setStatus(MembershipStatus.EXPIRED);
                if (obligation.getPlannedMembershipStart() != null) {
                    member.setMembershipEnd(obligation.getPlannedMembershipStart().minusDays(1));
                }
                memberRepository.save(member);
            }
            obligationRepository.delete(obligation);
        } else {
            obligationRepository.save(obligation);
        }
    }

    private MembershipPaymentOutcomeResponse buildOutcome(
            BillingPayment billing, MembershipObligation obligation) {
        MembershipObligationResponse obligationResponse = MembershipObligationMapper.toResponse(obligation);
        long balance = obligationResponse.balance();
        BigDecimal balanceAmount = BigDecimal.valueOf(balance);
        Member member = obligation.getMember();
        boolean activated = balance <= 0
                || !MembershipInstallmentAccessRules.mustStayInactive(member, balanceAmount, GYM_ZONE);
        String message;
        if (balance <= 0) {
            message = "Pago completo registrado. Membresía al día.";
        } else {
            message = "Abono registrado. Saldo pendiente de $"
                    + MoneyUtil.formatPesos(BigDecimal.valueOf(balance))
                    + ". Debe pagar todo antes de los "
                    + MembershipInstallmentAccessRules.INSTALLMENT_DEADLINE_DAYS
                    + " días al vencimiento; después no podrá ingresar hasta liquidar.";
        }
        return new MembershipPaymentOutcomeResponse(
                com.gym.management.mapper.BillingPaymentMapper.toResponse(billing),
                obligation.getStatus() == MembershipObligationStatus.OPEN ? obligationResponse : null,
                activated,
                balance,
                message);
    }

    private BillingPayment saveMembershipBillingPayment(
            Member member,
            MembershipPlan plan,
            int monthsPaid,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            BillingCashRegister cashRegister,
            Employee employee,
            MembershipObligation obligation,
            MembershipPaymentKind kind,
            MembershipDates dates) {

        String monthsLabel = monthsPaid == 1 ? "1 mes" : monthsPaid + " meses";
        String kindLabel = kind == MembershipPaymentKind.FULL ? "Pago completo" : "Abono";
        BigDecimal balance = MembershipObligationMapper.balanceOf(obligation);
        String notes = kind == MembershipPaymentKind.FULL
                ? kindLabel + " · Membresía " + plan.getName() + " · " + monthsLabel
                : kindLabel
                        + " · Membresía "
                        + plan.getName()
                        + " · "
                        + monthsLabel
                        + " · Saldo pendiente $"
                        + MoneyUtil.formatPesos(balance);

        return billingPaymentRepository.save(BillingPayment.builder()
                .paymentType(BillingPaymentType.MEMBERSHIP)
                .billingCashRegister(cashRegister)
                .member(member)
                .plan(plan)
                .employee(employee)
                .paymentMethod(paymentMethod)
                .amount(amount)
                .paymentDate(LocalDate.now(GYM_ZONE))
                .membershipStart(dates.start())
                .membershipEnd(dates.end())
                .membershipObligation(obligation)
                .membershipPaymentKind(kind)
                .notes(notes)
                .build());
    }

    private BillingPayment saveMembershipBillingPayments(
            Member member,
            MembershipPlan plan,
            int monthsPaid,
            List<PaymentSplitLine> splits,
            BillingCashRegister cashRegister,
            Employee employee,
            MembershipObligation obligation,
            MembershipPaymentKind kind,
            MembershipDates dates) {

        BillingPayment first = null;
        for (PaymentSplitLine line : splits) {
            BillingPayment saved = saveMembershipBillingPayment(
                    member,
                    plan,
                    monthsPaid,
                    line.paymentMethod(),
                    BigDecimal.valueOf(line.amount()),
                    cashRegister,
                    employee,
                    obligation,
                    kind,
                    dates);
            if (first == null) {
                first = saved;
            }
        }
        return first;
    }

    private List<PaymentSplitLine> resolvePaymentSplits(
            PaymentMethod paymentMethod, Long amountPesos, List<PaymentSplitLine> paymentSplits) {
        BigDecimal expectedTotal = requirePositivePesos(amountPesos);
        if (paymentSplits == null || paymentSplits.isEmpty()) {
            BillingPaymentMethodRules.requireAllowed(paymentMethod);
            return List.of(new PaymentSplitLine(paymentMethod, expectedTotal.longValue()));
        }
        if (paymentSplits.size() > 2) {
            throw new BusinessException("Máximo dos medios de pago por cobro");
        }
        if (paymentSplits.size() == 1) {
            PaymentSplitLine only = paymentSplits.get(0);
            BillingPaymentMethodRules.requireAllowed(only.paymentMethod());
            BigDecimal lineAmount = requirePositivePesos(only.amount());
            if (lineAmount.compareTo(expectedTotal) != 0) {
                throw new BusinessException(
                        "El monto del medio de pago ($"
                                + MoneyUtil.formatPesos(lineAmount)
                                + ") debe igualar el total ($"
                                + MoneyUtil.formatPesos(expectedTotal)
                                + ")");
            }
            return List.of(only);
        }
        PaymentSplitLine first = paymentSplits.get(0);
        PaymentSplitLine second = paymentSplits.get(1);
        if (first.paymentMethod() == second.paymentMethod()) {
            throw new BusinessException("Los dos medios de pago deben ser distintos");
        }
        BillingPaymentMethodRules.requireAllowed(first.paymentMethod());
        BillingPaymentMethodRules.requireAllowed(second.paymentMethod());
        BigDecimal sum = requirePositivePesos(first.amount()).add(requirePositivePesos(second.amount()));
        if (sum.compareTo(expectedTotal) != 0) {
            throw new BusinessException(
                    "La suma de los dos medios ($"
                            + MoneyUtil.formatPesos(sum)
                            + ") debe igualar el monto del cobro ($"
                            + MoneyUtil.formatPesos(expectedTotal)
                            + ")");
        }
        return List.of(first, second);
    }

    private static void requireSplitAllowedForFullPayment(
            List<PaymentSplitLine> paymentSplits,
            BigDecimal paymentAmount,
            BigDecimal planTotal,
            Long existingObligationId) {
        if (paymentSplits == null || paymentSplits.size() < 2) {
            return;
        }
        if (paymentAmount.compareTo(planTotal) > 0) {
            throw new BusinessException(
                    "El monto a dividir no puede superar el total de la membresía ($"
                            + MoneyUtil.formatPesos(planTotal)
                            + ")");
        }
    }

    private void applyMembershipToMember(
            Member member, MembershipPlan plan, MembershipDates dates, BigDecimal installmentBalance) {
        member.setPlan(plan);
        if (member.getMembershipStart() == null) {
            member.setMembershipStart(dates.start());
        }
        member.setMembershipEnd(dates.end());
        if (MembershipInstallmentAccessRules.mustStayInactive(member, installmentBalance, GYM_ZONE)) {
            member.setStatus(MembershipStatus.EXPIRED);
        } else {
            member.setStatus(MembershipStatus.ACTIVE);
        }
        member.setMembershipFrozen(false);
        member.setFrozenRemainingDays(null);
    }

    static MembershipDates computeMembershipDates(Member member, MembershipPlan plan, int monthsPaid) {
        LocalDate today = LocalDate.now(GYM_ZONE);
        LocalDate start = today;
        LocalDate currentEnd = member.getMembershipEnd();
        if (currentEnd != null && !currentEnd.isBefore(today)) {
            start = currentEnd.plusDays(1);
        } else {
            member.setMembershipStart(today);
        }
        long totalDays = (long) plan.getDurationDays() * monthsPaid;
        LocalDate end = start.plusDays(totalDays);
        return new MembershipDates(start, end);
    }

    private static boolean membershipMatchesObligation(Member member, MembershipObligation obligation) {
        Long memberPlanId = member.getPlan() != null ? member.getPlan().getId() : null;
        Long obligationPlanId = obligation.getPlan() != null ? obligation.getPlan().getId() : null;
        return java.util.Objects.equals(memberPlanId, obligationPlanId)
                && java.util.Objects.equals(member.getMembershipStart(), obligation.getPlannedMembershipStart())
                && java.util.Objects.equals(member.getMembershipEnd(), obligation.getPlannedMembershipEnd())
                && member.getStatus() == MembershipStatus.ACTIVE;
    }

    private static BigDecimal requirePositivePesos(Long amountPesos) {
        if (amountPesos == null || amountPesos < 1) {
            throw new BusinessException("Indique el monto del abono o pago en pesos (sin decimales)");
        }
        return BigDecimal.valueOf(amountPesos);
    }

    record MembershipDates(LocalDate start, LocalDate end) {}
}
