package com.gym.management.service;

import com.gym.management.dto.AttendanceSummaryResponse;
import com.gym.management.dto.WorkAttendanceRequest;
import com.gym.management.dto.WorkAttendanceResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.WorkAttendanceMapper;
import com.gym.management.model.Employee;
import com.gym.management.model.PayrollConfig;
import com.gym.management.model.UserRole;
import com.gym.management.model.WorkAttendance;
import com.gym.management.repository.WorkAttendanceRepository;
import com.gym.management.security.AuthenticatedUser;
import com.gym.management.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
    public List<WorkAttendanceResponse> findAll(Long employeeId, Integer year, Integer month, LocalDate date) {
        Long effectiveEmployeeId = resolveEmployeeFilter(employeeId);
        DateRange range = resolveDateRange(year, month, date);
        List<WorkAttendance> records = effectiveEmployeeId != null
                ? attendanceRepository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDescClockInDesc(
                        effectiveEmployeeId, range.from(), range.to())
                : attendanceRepository.findByWorkDateBetweenOrderByWorkDateDescClockInDesc(
                        range.from(), range.to());
        return records.stream().map(WorkAttendanceMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getSummary(Long employeeId, Integer year, Integer month, LocalDate date) {
        Long effectiveEmployeeId = resolveEmployeeFilter(employeeId);
        DateRange range = resolveDateRange(year, month, date);
        return new AttendanceSummaryResponse(
                attendanceRepository.countInPeriod(effectiveEmployeeId, range.from(), range.to()),
                attendanceRepository.countOpenInPeriod(effectiveEmployeeId, range.from(), range.to()),
                attendanceRepository.sumHoursInPeriod(effectiveEmployeeId, range.from(), range.to()),
                attendanceRepository.sumOwedInPeriod(effectiveEmployeeId, range.from(), range.to()));
    }

    @Transactional
    public WorkAttendanceResponse clockIn(Long employeeId) {
        Long effectiveEmployeeId = resolveEmployeeFilter(employeeId);
        if (effectiveEmployeeId == null) {
            throw new BusinessException("Debe indicar el empleado");
        }
        return clockInForEmployee(effectiveEmployeeId);
    }

    @Transactional
    public WorkAttendanceResponse clockOut(Long id) {
        WorkAttendance attendance = getAttendance(id);
        ensureCanManageEmployee(attendance.getEmployee().getId());
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
        ensureCanManageEmployee(request.employeeId());
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
        ensureCanManageEmployee(attendance.getEmployee().getId());
        ensureCanManageEmployee(request.employeeId());
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
        WorkAttendance attendance = getAttendance(id);
        ensureCanManageEmployee(attendance.getEmployee().getId());
        validateWorkDateIsToday(attendance.getWorkDate());
        attendanceRepository.delete(attendance);
    }

    private WorkAttendanceResponse clockInForEmployee(Long employeeId) {
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

    private Long resolveEmployeeFilter(Long requestedEmployeeId) {
        AuthenticatedUser user = requireUser();
        if (SecurityUtils.isAdmin()) {
            return requestedEmployeeId;
        }
        if (requestedEmployeeId != null && !requestedEmployeeId.equals(user.employeeId())) {
            throw new BusinessException("No tienes permiso para ver jornadas de otro empleado");
        }
        return user.employeeId();
    }

    private void ensureCanManageEmployee(Long employeeId) {
        AuthenticatedUser user = requireUser();
        if (SecurityUtils.isAdmin()) {
            return;
        }
        if (!user.employeeId().equals(employeeId)) {
            throw new BusinessException("Solo puedes gestionar tus propias jornadas");
        }
    }

    private AuthenticatedUser requireUser() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user == null) {
            throw new BusinessException("Sesión no válida");
        }
        if (user.role() != UserRole.ADMIN
                && user.role() != UserRole.SUPER_ADMIN
                && user.role() != UserRole.TRAINER) {
            throw new BusinessException("No tienes permiso para gestionar jornadas");
        }
        return user;
    }

    private DateRange resolveDateRange(Integer year, Integer month, LocalDate date) {
        if (date != null) {
            return new DateRange(date, date);
        }
        if (year != null && month != null) {
            validateMonth(year, month);
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            return new DateRange(start, end);
        }
        LocalDate today = LocalDate.now();
        LocalDate start = today.withDayOfMonth(1);
        LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
        return new DateRange(start, end);
    }

    private void validateMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException("Mes inválido");
        }
        YearMonth selected = YearMonth.of(year, month);
        YearMonth current = YearMonth.now();
        if (selected.isAfter(current)) {
            throw new BusinessException("No se puede consultar un mes futuro");
        }
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

    private record DateRange(LocalDate from, LocalDate to) {}
}
