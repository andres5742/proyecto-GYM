package com.gym.management.mapper;

import com.gym.management.dto.MemberResponse;
import com.gym.management.model.Member;

public final class MemberMapper {

    private MemberMapper() {}

    public static MemberResponse toResponse(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getFirstName(),
                member.getLastName(),
                member.getEmail(),
                member.getPhone(),
                member.getDocumentId(),
                member.getPlan() != null ? member.getPlan().getId() : null,
                member.getPlan() != null ? member.getPlan().getName() : null,
                member.getStatus(),
                member.getMembershipStart(),
                member.getMembershipEnd(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
