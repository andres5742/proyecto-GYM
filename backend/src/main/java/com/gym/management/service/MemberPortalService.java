package com.gym.management.service;

import com.gym.management.dto.AdminSetMemberPasswordRequest;
import com.gym.management.dto.ChangePasswordRequest;
import com.gym.management.dto.MemberPortalProfileResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.Member;
import com.gym.management.model.MembershipStatus;
import com.gym.management.repository.MemberRepository;
import com.gym.management.security.AuthenticatedUser;
import com.gym.management.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberPortalService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MemberPortalProfileResponse myProfile() {
        Member member = getCurrentMember();
        MembershipStatus status = MemberMembershipRules.effectiveStatus(member);
        return new MemberPortalProfileResponse(
                member.getId(),
                member.getFirstName(),
                member.getLastName(),
                member.getDocumentId(),
                member.getGender(),
                member.getPhone(),
                member.getPlan() != null ? member.getPlan().getName() : null,
                status,
                statusLabel(status),
                member.getMembershipStart(),
                member.getMembershipEnd());
    }

    @Transactional
    public void changeOwnPassword(ChangePasswordRequest request) {
        Member member = getCurrentMember();
        if (!passwordEncoder.matches(request.currentPassword(), member.getPasswordHash())) {
            throw new BusinessException("La contraseña actual no es correcta");
        }
        member.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        memberRepository.save(member);
    }

    @Transactional
    public void setPasswordByAdmin(Long memberId, AdminSetMemberPasswordRequest request) {
        if (!SecurityUtils.isAdmin()) {
            throw new BusinessException("No tienes permiso para cambiar la contraseña del afiliado");
        }
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + memberId));
        member.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        member.setPortalAccessEnabled(true);
        memberRepository.save(member);
    }

    @Transactional
    public void resetPasswordToDocument(Long memberId) {
        if (!SecurityUtils.isAdmin()) {
            throw new BusinessException("No tienes permiso para restablecer la contraseña");
        }
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + memberId));
        MemberService.applyDefaultPortalPassword(member, passwordEncoder);
        memberRepository.save(member);
    }

    private Member getCurrentMember() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user == null || user.memberId() == null) {
            throw new BusinessException("Sesión de afiliado no válida");
        }
        Member member = memberRepository
                .findById(user.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado"));
        if (MemberMembershipRules.effectiveStatus(member) != MembershipStatus.ACTIVE) {
            throw new BusinessException("Tu membresía no está activa");
        }
        return member;
    }

    private static String statusLabel(MembershipStatus status) {
        return switch (status) {
            case ACTIVE -> "Activo";
            case EXPIRED -> "Inactivo";
            case SUSPENDED -> "Suspendido";
        };
    }
}
