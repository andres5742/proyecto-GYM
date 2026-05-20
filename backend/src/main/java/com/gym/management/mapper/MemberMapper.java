package com.gym.management.mapper;

import com.gym.management.dto.MemberResponse;
import com.gym.management.model.Member;
import com.gym.management.service.MemberMembershipRules;

public final class MemberMapper {

    private MemberMapper() {}

    public static MemberResponse toResponse(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getFirstName(),
                member.getLastName(),
                member.getGender(),
                member.getPhone(),
                member.getDocumentId(),
                member.getPlan() != null ? member.getPlan().getId() : null,
                member.getPlan() != null ? member.getPlan().getName() : null,
                MemberMembershipRules.effectiveStatus(member),
                member.getMembershipStart(),
                member.getMembershipEnd(),
                Boolean.TRUE.equals(member.getMembershipFrozen()),
                member.getFrozenRemainingDays(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
