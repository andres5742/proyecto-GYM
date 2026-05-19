package com.gym.management.config;

import com.gym.management.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberExpirationScheduler {

    private final MemberService memberService;

    /** Marca como vencidos los afiliados cuya fecha de fin ya pasó. */
    @Scheduled(cron = "0 0 6 * * *")
    public void expireMembershipsDaily() {
        memberService.syncExpiredMemberships();
    }
}
