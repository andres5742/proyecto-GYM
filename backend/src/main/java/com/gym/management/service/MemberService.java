package com.gym.management.service;

import com.gym.management.dto.MemberBulkDeleteResponse;
import com.gym.management.dto.MemberRequest;
import com.gym.management.dto.MemberResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.MemberMapper;
import com.gym.management.model.Member;
import com.gym.management.model.MembershipPlan;
import com.gym.management.model.MembershipStatus;
import com.gym.management.repository.AccessLogRepository;
import com.gym.management.repository.MemberFingerprintRepository;
import com.gym.management.repository.MemberRepository;
import com.gym.management.security.SecurityUtils;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberFingerprintRepository fingerprintRepository;
    private final AccessLogRepository accessLogRepository;
    private final MembershipPlanService planService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public List<MemberResponse> findAll() {
        memberRepository.markExpiredActiveMembers(LocalDate.now());
        return memberRepository.findAll().stream()
                .map(MemberMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse findById(Long id) {
        return MemberMapper.toResponse(getMember(id));
    }

    @Transactional
    public MemberResponse create(MemberRequest request) {
        String email = resolveEmail(request, null);
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException("Ya existe un afiliado con ese documento o correo");
        }
        Member member = mapRequest(new Member(), request);
        return MemberMapper.toResponse(memberRepository.save(member));
    }

    @Transactional
    public MemberResponse update(Long id, MemberRequest request) {
        Member member = getMember(id);
        String email = resolveEmail(request, member);
        if (memberRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessException("Ya existe un afiliado con ese documento o correo");
        }
        mapRequest(member, request);
        return MemberMapper.toResponse(memberRepository.save(member));
    }

    @Transactional
    public void delete(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new ResourceNotFoundException("Afiliado no encontrado: " + id);
        }
        fingerprintRepository.findByMemberId(id).ifPresent(fingerprintRepository::delete);
        memberRepository.deleteById(id);
    }

    @Transactional
    public MemberBulkDeleteResponse deleteAll() {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException("Solo el super administrador puede borrar todos los afiliados");
        }
        long count = memberRepository.count();
        if (count == 0) {
            return new MemberBulkDeleteResponse(0);
        }
        fingerprintRepository.deleteAllInBatch();
        accessLogRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
        return new MemberBulkDeleteResponse(count);
    }

    private Member mapRequest(Member member, MemberRequest request) {
        member.setFirstName(request.firstName());
        member.setLastName(request.lastName());
        member.setEmail(resolveEmail(request, member));
        member.setPhone(request.phone());
        member.setDocumentId(request.documentId());
        member.setGender(request.gender());
        if (request.planId() != null) {
            MembershipPlan plan = planService.getPlan(request.planId());
            member.setPlan(plan);
            LocalDate start = request.membershipStart() != null
                    ? request.membershipStart()
                    : LocalDate.now();
            member.setMembershipStart(start);
            member.setMembershipEnd(request.membershipEnd() != null
                    ? request.membershipEnd()
                    : start.plusDays(plan.getDurationDays()));
        } else {
            member.setPlan(null);
            member.setMembershipStart(request.membershipStart());
            member.setMembershipEnd(request.membershipEnd());
        }

        MembershipStatus requested = request.status() != null ? request.status() : MembershipStatus.ACTIVE;
        member.setStatus(requested);
        MemberMembershipRules.applyExpirationStatus(member);
        applyDefaultPortalPassword(member, passwordEncoder);
        return member;
    }

    public static void applyDefaultPortalPassword(Member member, PasswordEncoder passwordEncoder) {
        if (member.getDocumentId() == null || member.getDocumentId().isBlank()) {
            return;
        }
        String document = member.getDocumentId().trim();
        member.setPasswordHash(passwordEncoder.encode(document));
        member.setPortalAccessEnabled(true);
    }

    @Transactional
    public void syncExpiredMemberships() {
        memberRepository.markExpiredActiveMembers(LocalDate.now());
    }

    private String resolveEmail(MemberRequest request, Member existing) {
        if (request.email() != null && !request.email().isBlank()) {
            return request.email().trim().toLowerCase();
        }
        if (request.documentId() != null && !request.documentId().isBlank()) {
            return request.documentId().replaceAll("\\s+", "") + "@sin-correo.importado";
        }
        if (existing != null && existing.getEmail() != null) {
            return existing.getEmail();
        }
        return "afiliado-" + System.currentTimeMillis() + "@sin-correo.importado";
    }

    private Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + id));
    }
}
