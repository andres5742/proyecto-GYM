package com.gym.management.repository;

import com.gym.management.model.ShiftStatus;
import com.gym.management.model.WorkShift;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {

    @EntityGraph(attributePaths = "employee")
    Optional<WorkShift> findFirstByStatusOrderByOpenedAtDesc(ShiftStatus status);

    @EntityGraph(attributePaths = "employee")
    List<WorkShift> findAllByOrderByShiftDateDescOpenedAtDesc();

    @EntityGraph(attributePaths = "employee")
    Optional<WorkShift> findWithEmployeeById(Long id);

    boolean existsByStatus(ShiftStatus status);
}
