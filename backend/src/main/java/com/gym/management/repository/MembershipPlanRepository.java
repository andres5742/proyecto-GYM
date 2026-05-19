package com.gym.management.repository;

import com.gym.management.model.MembershipPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<MembershipPlan> findByNameIgnoreCase(String name);
}
