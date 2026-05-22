package com.gym.management.service;

import com.gym.management.dto.AccessVoiceHints;
import com.gym.management.model.Member;
import com.gym.management.model.MembershipPlan;
import com.gym.management.repository.AccessLogRepository;
import java.time.ZoneId;

public final class AccessVoiceHintsBuilder {

    private AccessVoiceHintsBuilder() {}

    public static AccessVoiceHints forMember(
            Member member, AccessLogRepository accessLogRepository, ZoneId zone, boolean accessLogAlreadySaved) {
        if (member == null) {
            return AccessVoiceHints.none();
        }

        Integer membershipDaysRemaining = null;
        int daysUntilEnd = MembershipFreezeService.remainingDaysUntilEnd(member.getMembershipEnd());
        if (daysUntilEnd > 0 && daysUntilEnd <= AccessVoiceHints.MEMBERSHIP_WARNING_DAYS) {
            membershipDaysRemaining = daysUntilEnd;
        }

        MembershipPlan plan = member.getPlan();
        boolean tiquetera = TicketBookAccessRules.isTiqueteraPlan(plan);
        Integer tiqueteraEntriesRemainingAfter = null;
        if (tiquetera) {
            int remaining = TicketBookAccessRules.remainingEntries(member, accessLogRepository, zone);
            if (remaining >= 0) {
                int after = accessLogAlreadySaved ? remaining : remaining - 1;
                if (after >= 0) {
                    tiqueteraEntriesRemainingAfter = after;
                }
            }
        }

        return new AccessVoiceHints(membershipDaysRemaining, tiqueteraEntriesRemainingAfter, tiquetera);
    }
}
