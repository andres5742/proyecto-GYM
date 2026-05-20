package com.gym.management.service;

import com.gym.management.model.AccessResult;
import com.gym.management.model.Member;
import com.gym.management.model.MembershipPlan;
import com.gym.management.model.MembershipPlanKind;
import com.gym.management.repository.AccessLogRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

public final class TicketBookAccessRules {

    private TicketBookAccessRules() {}

    public static boolean isTiqueteraPlan(MembershipPlan plan) {
        return plan != null && plan.getPlanKind() == MembershipPlanKind.TIQUETERA;
    }

    /** Cupo total de entrenos del periodo de membresía pagado (no días calendario). */
    public static int entryLimit(Member member) {
        MembershipPlan plan = member.getPlan();
        if (!isTiqueteraPlan(plan)) {
            return -1;
        }
        if (plan.getMonthlyEntryLimit() != null && plan.getMonthlyEntryLimit() > 0) {
            return plan.getMonthlyEntryLimit();
        }
        return 0;
    }

    public static long usedEntriesInMembershipPeriod(
            Member member, AccessLogRepository accessLogRepository, ZoneId zone) {
        LocalDate start = member.getMembershipStart();
        LocalDate end = member.getMembershipEnd();
        if (start == null || end == null) {
            return 0;
        }
        Instant from = start.atStartOfDay(zone).toInstant();
        Instant to = end.plusDays(1).atStartOfDay(zone).toInstant();
        return accessLogRepository.countByMemberIdAndResultBetween(
                member.getId(), AccessResult.GRANTED, from, to);
    }

    public static int remainingEntries(
            Member member, AccessLogRepository accessLogRepository, ZoneId zone) {
        int limit = entryLimit(member);
        if (limit < 0) {
            return -1;
        }
        long used = usedEntriesInMembershipPeriod(member, accessLogRepository, zone);
        return Math.max(0, limit - (int) used);
    }

    public static Optional<String> denyReasonIfMonthlyLimitReached(
            Member member, AccessLogRepository accessLogRepository, ZoneId zone) {
        if (!isTiqueteraPlan(member.getPlan())) {
            return Optional.empty();
        }
        LocalDate today = LocalDate.now(zone);
        if (member.getMembershipStart() != null && member.getMembershipStart().isAfter(today)) {
            return Optional.of("Tu tiquetera aún no ha iniciado.");
        }
        if (member.getMembershipEnd() != null && member.getMembershipEnd().isBefore(today)) {
            return Optional.of("Tu tiquetera está vencida. Renueva en recepción.");
        }

        int limit = entryLimit(member);
        if (limit <= 0) {
            return Optional.of(
                    "Tu plan tiquetera no tiene cupo de entrenos configurado. Acércate a recepción.");
        }

        long used = usedEntriesInMembershipPeriod(member, accessLogRepository, zone);
        if (used >= limit) {
            return Optional.of(
                    "Ya no tienes entrenos en tu tiquetera. Has usado los "
                            + limit
                            + " entrenos de tu plan.");
        }
        return Optional.empty();
    }

    /** @deprecated Usar {@link #remainingEntries}; conservado por compatibilidad interna. */
    @Deprecated
    public static int remainingEntriesThisMonth(
            Member member, AccessLogRepository accessLogRepository, ZoneId zone) {
        return remainingEntries(member, accessLogRepository, zone);
    }
}
