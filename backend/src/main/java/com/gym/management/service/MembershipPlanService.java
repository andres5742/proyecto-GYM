package com.gym.management.service;

import com.gym.management.dto.MembershipPlanRequest;
import com.gym.management.dto.MembershipPlanResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.MembershipPlanMapper;
import com.gym.management.model.MembershipPlan;
import com.gym.management.repository.MemberRepository;
import com.gym.management.repository.MembershipPlanRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipPlanService {

    private final MembershipPlanRepository planRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<MembershipPlanResponse> findAll() {
        return planRepository.findAll().stream()
                .map(MembershipPlanMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MembershipPlanResponse findById(Long id) {
        return MembershipPlanMapper.toResponse(getPlan(id));
    }

    @Transactional
    public MembershipPlanResponse create(MembershipPlanRequest request) {
        if (planRepository.existsByName(request.name())) {
            throw new BusinessException("Ya existe un plan con ese nombre");
        }
        MembershipPlan plan = MembershipPlan.builder()
                .name(request.name())
                .description(request.description())
                .durationDays(request.durationDays())
                .price(request.price())
                .active(request.active() != null ? request.active() : true)
                .build();
        return MembershipPlanMapper.toResponse(planRepository.save(plan));
    }

    @Transactional
    public MembershipPlanResponse update(Long id, MembershipPlanRequest request) {
        MembershipPlan plan = getPlan(id);
        if (planRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new BusinessException("Ya existe un plan con ese nombre");
        }
        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setDurationDays(request.durationDays());
        plan.setPrice(request.price());
        if (request.active() != null) {
            plan.setActive(request.active());
        }
        return MembershipPlanMapper.toResponse(planRepository.save(plan));
    }

    @Transactional
    public void delete(Long id) {
        MembershipPlan plan = getPlan(id);
        memberRepository.detachPlanFromMembers(plan.getId());
        planRepository.delete(plan);
    }

    public MembershipPlan getPlan(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado: " + id));
    }
}
