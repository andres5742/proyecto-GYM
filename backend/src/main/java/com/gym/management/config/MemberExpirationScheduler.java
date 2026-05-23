package com.gym.management.config;

import com.gym.management.repository.MemberRepository;
import com.gym.management.repository.MembershipObligationRepository;
import com.gym.management.service.MemberService;
import com.gym.management.service.MembershipInstallmentAccessRules;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberExpirationScheduler {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private final MemberService memberService;
    private final MembershipObligationRepository membershipObligationRepository;
    private final MemberRepository memberRepository;

    /** Marca como vencidos los afiliados cuya fecha de fin ya pasó. */
    @Scheduled(cron = "0 0 6 * * *")
    public void expireMembershipsDaily() {
        memberService.syncExpiredMemberships();
        MembershipInstallmentAccessRules.syncInactiveForUnpaidInstallmentsNearExpiry(
                membershipObligationRepository, memberRepository, GYM_ZONE);
    }
}
