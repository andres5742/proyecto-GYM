package com.gym.management.repository;

import com.gym.management.model.WorkAttendance;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkAttendanceRepository extends JpaRepository<WorkAttendance, Long> {

    List<WorkAttendance> findAllByOrderByWorkDateDescClockInDesc();

    List<WorkAttendance> findByEmployeeIdOrderByWorkDateDescClockInDesc(Long employeeId);

    Optional<WorkAttendance> findByEmployeeIdAndWorkDateAndClockOutIsNull(Long employeeId, LocalDate workDate);

    @Query("SELECT COALESCE(SUM(a.amountOwed), 0) FROM WorkAttendance a WHERE a.clockOut IS NOT NULL")
    BigDecimal sumTotalOwed();

    @Query("SELECT COALESCE(SUM(a.hoursWorked), 0) FROM WorkAttendance a WHERE a.clockOut IS NOT NULL")
    BigDecimal sumTotalHours();

    long countByClockOutIsNull();
}
