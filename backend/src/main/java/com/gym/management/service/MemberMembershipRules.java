package com.gym.management.service;

import com.gym.management.model.Member;
import com.gym.management.model.MembershipStatus;
import java.time.LocalDate;
import java.time.ZoneId;

public final class MemberMembershipRules {
    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private MemberMembershipRules() {}

    public static boolean isMembershipExpired(LocalDate membershipEnd) {
        return membershipEnd != null && membershipEnd.isBefore(LocalDate.now(GYM_ZONE));
    }

    public static MembershipStatus effectiveStatus(Member member) {
        if (member.getStatus() == MembershipStatus.SUSPENDED) {
            return MembershipStatus.SUSPENDED;
        }
        if (isMembershipExpired(member.getMembershipEnd())) {
            return MembershipStatus.EXPIRED;
        }
        return member.getStatus() != null ? member.getStatus() : MembershipStatus.ACTIVE;
    }

    public static void applyExpirationStatus(Member member) {
        if (member.getStatus() == MembershipStatus.SUSPENDED) {
            return;
        }
        if (isMembershipExpired(member.getMembershipEnd())) {
            member.setStatus(MembershipStatus.EXPIRED);
        }
    }

    public static MembershipStatus resolveStatus(String rawExcel, LocalDate membershipEnd) {
        if (isMembershipExpired(membershipEnd)) {
            return MembershipStatus.EXPIRED;
        }
        if (rawExcel == null || rawExcel.isBlank()) {
            return MembershipStatus.ACTIVE;
        }
        String normalized = normalizeToken(rawExcel);
        return switch (normalized) {
            case "activo", "active", "a", "vigente" -> MembershipStatus.ACTIVE;
            case "vencido", "expirado", "expired", "e", "inactivo" -> MembershipStatus.EXPIRED;
            case "suspendido", "suspended", "s" -> MembershipStatus.SUSPENDED;
            default -> MembershipStatus.valueOf(rawExcel.trim().toUpperCase());
        };
    }

    private static String normalizeToken(String value) {
        String normalized = java.text.Normalizer.normalize(value.trim().toLowerCase(), java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }
}
