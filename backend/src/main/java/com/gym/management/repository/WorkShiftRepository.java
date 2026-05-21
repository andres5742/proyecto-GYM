package com.gym.management.repository;

import com.gym.management.model.ShiftStatus;
import com.gym.management.model.WorkShift;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {

    @EntityGraph(attributePaths = "employee")
    Optional<WorkShift> findFirstByStatusOrderByOpenedAtDesc(ShiftStatus status);

    @EntityGraph(attributePaths = "employee")
    List<WorkShift> findAllByOrderByShiftDateDescOpenedAtDesc();

    @EntityGraph(attributePaths = "employee")
    Optional<WorkShift> findWithEmployeeById(Long id);

    boolean existsByStatus(ShiftStatus status);

    @EntityGraph(attributePaths = "employee")
    List<WorkShift> findByShiftDateAndStatusAndOpenedAtBeforeOrderByOpenedAtAsc(
            LocalDate shiftDate, ShiftStatus status, LocalDateTime openedAt);

    long countByShiftDate(LocalDate shiftDate);

    @Query("SELECT COUNT(DISTINCT s.workShift.id) FROM Sale s WHERE s.workShift.shiftDate = :date")
    long countShiftsWithSalesByShiftDate(@Param("date") LocalDate date);

    @EntityGraph(attributePaths = "employee")
    Optional<WorkShift> findFirstByShiftDateAndStatusOrderByClosedAtDesc(
            LocalDate shiftDate, ShiftStatus status);

    @EntityGraph(attributePaths = "employee")
    Optional<WorkShift> findFirstByShiftDateAndStatusOrderByOpenedAtDesc(
            LocalDate shiftDate, ShiftStatus status);
}
