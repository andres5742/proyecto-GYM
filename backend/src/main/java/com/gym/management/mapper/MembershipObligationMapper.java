package com.gym.management.mapper;

import com.gym.management.dto.MembershipObligationResponse;
import com.gym.management.model.MembershipObligation;
import com.gym.management.util.MoneyUtil;
import java.math.BigDecimal;

public final class MembershipObligationMapper {

    private MembershipObligationMapper() {}

    public static MembershipObligationResponse toResponse(MembershipObligation obligation) {
        long total = MoneyUtil.roundPesos(obligation.getTotalAmount()).longValue();
        long paid = MoneyUtil.roundPesos(obligation.getAmountPaid()).longValue();
        long balance = Math.max(0, total - paid);
        String memberName = obligation.getMember().getFirstName() + " " + obligation.getMember().getLastName();
        return new MembershipObligationResponse(
                obligation.getId(),
                obligation.getMember().getId(),
                memberName,
                obligation.getPlan().getId(),
                obligation.getPlan().getName(),
                obligation.getMonthsPaid(),
                total,
                paid,
                balance,
                obligation.getStatus(),
                obligation.getPlannedMembershipStart(),
                obligation.getPlannedMembershipEnd());
    }

    public static BigDecimal balanceOf(MembershipObligation obligation) {
        return MoneyUtil.roundPesos(obligation.getTotalAmount().subtract(obligation.getAmountPaid()));
    }
}
