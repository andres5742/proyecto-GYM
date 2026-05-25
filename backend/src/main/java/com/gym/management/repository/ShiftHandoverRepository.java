package com.gym.management.repository;

import com.gym.management.model.ShiftHandover;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftHandoverRepository extends JpaRepository<ShiftHandover, Long> {

    boolean existsByWorkShiftId(Long workShiftId);

    Optional<ShiftHandover> findByWorkShiftId(Long workShiftId);

    @EntityGraph(attributePaths = {"workShift", "workShift.employee", "employee"})
    Optional<ShiftHandover> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"workShift", "workShift.employee", "employee"})
    List<ShiftHandover> findAllByOrderBySubmittedAtDesc();

    @EntityGraph(attributePaths = {"workShift", "workShift.employee", "employee"})
    List<ShiftHandover> findByEmployeeIdOrderBySubmittedAtDesc(Long employeeId);

    @EntityGraph(attributePaths = {"workShift", "workShift.employee", "employee"})
    Optional<ShiftHandover> findFirstByWorkShift_ShiftDateOrderBySubmittedAtDesc(LocalDate shiftDate);

    @EntityGraph(attributePaths = {"workShift", "workShift.employee", "employee"})
    List<ShiftHandover> findByWorkShift_ShiftDateOrderBySubmittedAtDesc(LocalDate shiftDate);

    /** Última entrega registrada antes de abrir la caja del día (p. ej. entrega nocturna del día anterior). */
    @EntityGraph(attributePaths = {"workShift", "workShift.employee", "employee"})
    Optional<ShiftHandover> findFirstBySubmittedAtLessThanOrderBySubmittedAtDesc(Instant before);
}
