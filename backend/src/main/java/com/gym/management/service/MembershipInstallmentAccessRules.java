package com.gym.management.service;

import com.gym.management.mapper.MembershipObligationMapper;
import com.gym.management.model.Member;
import com.gym.management.model.MembershipObligation;
import com.gym.management.model.MembershipObligationStatus;
import com.gym.management.model.MembershipStatus;
import com.gym.management.repository.MemberRepository;
import com.gym.management.repository.MembershipObligationRepository;
import com.gym.management.util.MoneyUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public final class MembershipInstallmentAccessRules {

    /** Días antes del vencimiento en que debe estar pagada la membresía si hubo abonos. */
    public static final int INSTALLMENT_DEADLINE_DAYS = 10;

    private MembershipInstallmentAccessRules() {}

    public static Optional<String> denyReasonIfUnpaidInstallmentDue(
            Member member, MembershipObligationRepository obligationRepository, ZoneId zone) {
        return findOpenBalance(obligationRepository, member.getId())
                .flatMap(balance -> denyMessageIfNearExpiry(member, balance, zone));
    }

    public static boolean mustStayInactive(Member member, BigDecimal installmentBalance, ZoneId zone) {
        if (installmentBalance == null || installmentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return isWithinInstallmentDeadline(member, zone);
    }

    public static void syncInactiveForUnpaidInstallmentsNearExpiry(
            MembershipObligationRepository obligationRepository, MemberRepository memberRepository, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        List<MembershipObligation> openObligations =
                obligationRepository.findAllOpenWithDetails(MembershipObligationStatus.OPEN);
        for (MembershipObligation obligation : openObligations) {
            BigDecimal balance = MembershipObligationMapper.balanceOf(obligation);
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Member member = obligation.getMember();
            if (member.getStatus() == MembershipStatus.SUSPENDED) {
                continue;
            }
            LocalDate end = member.getMembershipEnd();
            if (end == null || end.isBefore(today)) {
                continue;
            }
            long daysRemaining = ChronoUnit.DAYS.between(today, end);
            if (daysRemaining <= INSTALLMENT_DEADLINE_DAYS && member.getStatus() == MembershipStatus.ACTIVE) {
                member.setStatus(MembershipStatus.EXPIRED);
                memberRepository.save(member);
            }
        }
    }

    private static Optional<BigDecimal> findOpenBalance(
            MembershipObligationRepository obligationRepository, Long memberId) {
        return obligationRepository
                .findOpenByMemberIdWithDetails(memberId, MembershipObligationStatus.OPEN)
                .map(MembershipObligationMapper::balanceOf)
                .filter(balance -> balance.compareTo(BigDecimal.ZERO) > 0);
    }

    private static Optional<String> denyMessageIfNearExpiry(Member member, BigDecimal balance, ZoneId zone) {
        if (!isWithinInstallmentDeadline(member, zone)) {
            return Optional.empty();
        }
        return Optional.of(
                "Tienes saldo pendiente de $"
                        + MoneyUtil.formatPesos(balance)
                        + " por tu membresía. Debes pagar todo antes de los "
                        + INSTALLMENT_DEADLINE_DAYS
                        + " días al vencimiento para ingresar. Acércate a recepción.");
    }

    private static boolean isWithinInstallmentDeadline(Member member, ZoneId zone) {
        LocalDate end = member.getMembershipEnd();
        if (end == null) {
            return false;
        }
        LocalDate today = LocalDate.now(zone);
        if (end.isBefore(today)) {
            return false;
        }
        long daysRemaining = ChronoUnit.DAYS.between(today, end);
        return daysRemaining <= INSTALLMENT_DEADLINE_DAYS;
    }
}
