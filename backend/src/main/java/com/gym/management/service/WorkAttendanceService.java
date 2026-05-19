package com.gym.management.service;

import com.gym.management.dto.AttendanceSummaryResponse;
import com.gym.management.dto.WorkAttendanceRequest;
import com.gym.management.dto.WorkAttendanceResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.WorkAttendanceMapper;
import com.gym.management.model.Employee;
import com.gym.management.model.PayrollConfig;
import com.gym.management.model.WorkAttendance;
import com.gym.management.repository.WorkAttendanceRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkAttendanceService {

    private final WorkAttendanceRepository attendanceRepository;
    private final EmployeeService employeeService;
    private final PayrollConfigService payrollConfigService;

    @Transactional(readOnly = true)
    public List<WorkAttendanceResponse> findAll(Long employeeId) {
        List<WorkAttendance> records = employeeId != null
                ? attendanceRepository.findByEmployeeIdOrderByWorkDateDescClockInDesc(employeeId)
                : attendanceRepository.findAllByOrderByWorkDateDescClockInDesc();
        return records.stream().map(WorkAttendanceMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getSummary() {
        return new AttendanceSummaryResponse(
                attendanceRepository.count(),
                attendanceRepository.countByClockOutIsNull(),
                attendanceRepository.sumTotalHours(),
                attendanceRepository.sumTotalOwed()
        );
    }

    @Transactional
    public WorkAttendanceResponse clockIn(Long employeeId) {
        Employee employee = employeeService.getEmployee(employeeId);
        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new BusinessException("El empleado no está activo");
        }
        LocalDate today = LocalDate.now();
        if (attendanceRepository.findByEmployeeIdAndWorkDateAndClockOutIsNull(employeeId, today)
                .isPresent()) {
            throw new BusinessException("El empleado ya tiene una jornada abierta hoy");
        }

        boolean sunday = today.getDayOfWeek() == DayOfWeek.SUNDAY;
        WorkAttendance attendance = WorkAttendance.builder()
                .employee(employee)
                .workDate(today)
                .clockIn(normalizeToWholeHour(LocalDateTime.now()))
                .sunday(sunday)
                .build();
        return WorkAttendanceMapper.toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public WorkAttendanceResponse clockOut(Long id) {
        WorkAttendance attendance = getAttendance(id);
        validateWorkDateIsToday(attendance.getWorkDate());
        if (attendance.getClockOut() != null) {
            throw new BusinessException("La jornada ya fue cerrada");
        }
        attendance.setClockOut(normalizeToWholeHour(LocalDateTime.now()));
        applyPayrollCalculation(attendance);
        return WorkAttendanceMapper.toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public WorkAttendanceResponse create(WorkAttendanceRequest request) {
        Employee employee = employeeService.getEmployee(request.employeeId());
        LocalDate today = LocalDate.now();
        validateWorkDateIsToday(request.workDate());
        validateTimesOnToday(request.clockIn(), request.clockOut());

        WorkAttendance attendance = WorkAttendance.builder()
                .employee(employee)
                .workDate(today)
                .clockIn(normalizeToWholeHour(request.clockIn()))
                .clockOut(request.clockOut() != null ? normalizeToWholeHour(request.clockOut()) : null)
                .sunday(today.getDayOfWeek() == DayOfWeek.SUNDAY)
                .notes(request.notes())
                .build();

        if (request.clockOut() != null) {
            applyPayrollCalculation(attendance);
        }

        return WorkAttendanceMapper.toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public WorkAttendanceResponse update(Long id, WorkAttendanceRequest request) {
        WorkAttendance attendance = getAttendance(id);
        validateWorkDateIsToday(attendance.getWorkDate());
        validateWorkDateIsToday(request.workDate());
        Employee employee = employeeService.getEmployee(request.employeeId());
        LocalDate today = LocalDate.now();
        validateTimesOnToday(request.clockIn(), request.clockOut());

        attendance.setEmployee(employee);
        attendance.setWorkDate(today);
        attendance.setClockIn(normalizeToWholeHour(request.clockIn()));
        attendance.setClockOut(request.clockOut() != null ? normalizeToWholeHour(request.clockOut()) : null);
        attendance.setSunday(today.getDayOfWeek() == DayOfWeek.SUNDAY);
        attendance.setNotes(request.notes());

        if (request.clockOut() != null) {
            applyPayrollCalculation(attendance);
        } else {
            attendance.setHoursWorked(null);
            attendance.setHourlyRateApplied(null);
            attendance.setAmountOwed(null);
        }

        return WorkAttendanceMapper.toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public void delete(Long id) {
        if (!attendanceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Jornada no encontrada: " + id);
        }
        attendanceRepository.deleteById(id);
    }

    private void applyPayrollCalculation(WorkAttendance attendance) {
        if (attendance.getClockOut() == null) {
            return;
        }
        if (!attendance.getClockOut().isAfter(attendance.getClockIn())) {
            throw new BusinessException("La hora de salida debe ser posterior a la de entrada");
        }

        PayrollConfig config = payrollConfigService.getConfig();
        boolean sunday = attendance.getWorkDate().getDayOfWeek() == DayOfWeek.SUNDAY;
        BigDecimal rate = sunday ? config.getSundayHourlyRate() : config.getWeekdayHourlyRate();

        long wholeHours = ChronoUnit.HOURS.between(attendance.getClockIn(), attendance.getClockOut());
        BigDecimal hours = BigDecimal.valueOf(wholeHours);

        attendance.setSunday(sunday);
        attendance.setHoursWorked(hours);
        attendance.setHourlyRateApplied(rate);
        attendance.setAmountOwed(hours.multiply(rate));
    }

    private void validateWorkDateIsToday(LocalDate workDate) {
        LocalDate today = LocalDate.now();
        if (!today.equals(workDate)) {
            throw new BusinessException("Solo se permiten registros del día actual");
        }
    }

    private void validateTimesOnToday(LocalDateTime clockIn, LocalDateTime clockOut) {
        LocalDate today = LocalDate.now();
        if (!today.equals(clockIn.toLocalDate())) {
            throw new BusinessException("La hora de entrada debe ser del día actual");
        }
        if (clockOut != null && !today.equals(clockOut.toLocalDate())) {
            throw new BusinessException("La hora de salida debe ser del día actual");
        }
        if (clockOut != null && !clockOut.isAfter(clockIn)) {
            throw new BusinessException("La hora de salida debe ser posterior a la de entrada");
        }
    }

    private LocalDateTime normalizeToWholeHour(LocalDateTime dateTime) {
        return dateTime.withMinute(0).withSecond(0).withNano(0);
    }

    private WorkAttendance getAttendance(Long id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jornada no encontrada: " + id));
    }
}
