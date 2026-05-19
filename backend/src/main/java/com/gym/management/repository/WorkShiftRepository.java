package com.gym.management.repository;

import com.gym.management.model.ShiftStatus;
import com.gym.management.model.WorkShift;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {
    Optional<WorkShift> findFirstByStatusOrderByOpenedAtDesc(ShiftStatus status);

    List<WorkShift> findAllByOrderByShiftDateDescOpenedAtDesc();

    boolean existsByStatus(ShiftStatus status);
}
