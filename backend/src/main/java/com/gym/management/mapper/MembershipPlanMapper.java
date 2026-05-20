package com.gym.management.mapper;

import com.gym.management.dto.MembershipPlanResponse;
import com.gym.management.model.MembershipPlan;

public final class MembershipPlanMapper {

    private MembershipPlanMapper() {}

    public static MembershipPlanResponse toResponse(MembershipPlan plan) {
        return new MembershipPlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getDurationDays(),
                plan.getPlanKind(),
                plan.getMonthlyEntryLimit(),
                plan.getPrice(),
                plan.getActive(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
