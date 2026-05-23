package com.gym.management.config;

import com.gym.management.repository.MemberRepository;
import com.gym.management.repository.MembershipObligationRepository;
import com.gym.management.service.MemberService;
import com.gym.management.service.MembershipInstallmentAccessRules;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberExpirationStartup {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private final MemberService memberService;
    private final MembershipObligationRepository membershipObligationRepository;
    private final MemberRepository memberRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        memberService.syncExpiredMemberships();
        MembershipInstallmentAccessRules.syncInactiveForUnpaidInstallmentsNearExpiry(
                membershipObligationRepository, memberRepository, GYM_ZONE);
    }
}
