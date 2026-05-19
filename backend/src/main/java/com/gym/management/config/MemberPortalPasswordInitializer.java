package com.gym.management.config;

import com.gym.management.model.Member;
import com.gym.management.repository.MemberRepository;
import com.gym.management.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberPortalPasswordInitializer {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensurePortalPasswords() {
        for (Member member : memberRepository.findAll()) {
            if (member.getDocumentId() == null || member.getDocumentId().isBlank()) {
                continue;
            }
            if (member.getPasswordHash() == null || member.getPasswordHash().isBlank()) {
                MemberService.applyDefaultPortalPassword(member, passwordEncoder);
                memberRepository.save(member);
            }
        }
    }
}
