package com.gym.management.service;

import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.Member;
import com.gym.management.model.MembershipStatus;
import com.gym.management.repository.MemberRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipFreezeService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private final MemberRepository memberRepository;

    @Transactional
    public Member freeze(Long memberId) {
        Member member = getMember(memberId);
        if (Boolean.TRUE.equals(member.getMembershipFrozen())) {
            throw new BusinessException("La membresía ya está congelada");
        }
        if (member.getStatus() == MembershipStatus.SUSPENDED) {
            throw new BusinessException("No se puede congelar una membresía suspendida");
        }
        int remaining = remainingDaysUntilEnd(member.getMembershipEnd());
        if (remaining <= 0) {
            throw new BusinessException(
                    "No hay días restantes de membresía para congelar. Renueva o extienda la fecha de vencimiento.");
        }
        member.setMembershipFrozen(true);
        member.setFrozenRemainingDays(remaining);
        return memberRepository.save(member);
    }

    @Transactional
    public Member unfreeze(Long memberId) {
        Member member = getMember(memberId);
        if (!Boolean.TRUE.equals(member.getMembershipFrozen())) {
            throw new BusinessException("La membresía no está congelada");
        }
        Integer remaining = member.getFrozenRemainingDays();
        if (remaining == null || remaining <= 0) {
            throw new BusinessException("No hay días guardados para reactivar la membresía");
        }
        LocalDate today = LocalDate.now(GYM_ZONE);
        member.setMembershipEnd(today.plusDays(remaining));
        member.setMembershipFrozen(false);
        member.setFrozenRemainingDays(null);
        if (member.getStatus() != MembershipStatus.SUSPENDED) {
            member.setStatus(MembershipStatus.ACTIVE);
            MemberMembershipRules.applyExpirationStatus(member);
        }
        return memberRepository.save(member);
    }

    public static boolean isFrozen(Member member) {
        return member != null && Boolean.TRUE.equals(member.getMembershipFrozen());
    }

    public static int remainingDaysUntilEnd(LocalDate membershipEnd) {
        LocalDate today = LocalDate.now(GYM_ZONE);
        if (membershipEnd == null || membershipEnd.isBefore(today)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(today, membershipEnd);
    }

    private Member getMember(Long id) {
        return memberRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + id));
    }
}
