package com.gym.management.config;

import com.gym.management.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberExpirationStartup {

    private final MemberService memberService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        memberService.syncExpiredMemberships();
    }
}
