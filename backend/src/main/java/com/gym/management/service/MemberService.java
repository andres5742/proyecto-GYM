package com.gym.management.service;

import com.gym.management.dto.MemberRequest;
import com.gym.management.dto.MemberResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.MemberMapper;
import com.gym.management.model.Member;
import com.gym.management.model.MembershipPlan;
import com.gym.management.model.MembershipStatus;
import com.gym.management.repository.MemberRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MembershipPlanService planService;

    @Transactional(readOnly = true)
    public List<MemberResponse> findAll() {
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
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException("Ya existe un socio con ese correo");
        }
        Member member = mapRequest(new Member(), request);
        return MemberMapper.toResponse(memberRepository.save(member));
    }

    @Transactional
    public MemberResponse update(Long id, MemberRequest request) {
        Member member = getMember(id);
        if (memberRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new BusinessException("Ya existe un socio con ese correo");
        }
        mapRequest(member, request);
        return MemberMapper.toResponse(memberRepository.save(member));
    }

    @Transactional
    public void delete(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new ResourceNotFoundException("Socio no encontrado: " + id);
        }
        memberRepository.deleteById(id);
    }

    private Member mapRequest(Member member, MemberRequest request) {
        member.setFirstName(request.firstName());
        member.setLastName(request.lastName());
        member.setEmail(request.email());
        member.setPhone(request.phone());
        member.setDocumentId(request.documentId());
        member.setStatus(request.status() != null ? request.status() : MembershipStatus.ACTIVE);

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

        return member;
    }

    private Member getMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio no encontrado: " + id));
    }
}
