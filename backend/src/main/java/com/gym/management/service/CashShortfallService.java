package com.gym.management.service;

import com.gym.management.dto.CashShortfallMonthlySummaryResponse;
import com.gym.management.dto.CashShortfallResponse;
import com.gym.management.dto.SettleCashShortfallRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.CashShortfallStatus;
import com.gym.management.model.Employee;
import com.gym.management.model.EmployeeCashShortfall;
import com.gym.management.model.ShiftHandover;
import com.gym.management.repository.EmployeeCashShortfallRepository;
import com.gym.management.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CashShortfallService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private final EmployeeCashShortfallRepository shortfallRepository;
    private final EmployeeService employeeService;

    @Transactional
    public Optional<CashShortfallResponse> registerFromHandover(
            ShiftHandover handover, BigDecimal expectedAmount, BigDecimal declaredAmount) {
        if (handover.getId() == null) {
            return Optional.empty();
        }
        Optional<EmployeeCashShortfall> existing =
                shortfallRepository.findByShiftHandoverId(handover.getId());
        if (existing.isPresent()) {
            return existing.map(this::toResponse);
        }
        if (declaredAmount.compareTo(expectedAmount) >= 0) {
            return Optional.empty();
        }
        BigDecimal shortfall = expectedAmount.subtract(declaredAmount);
        EmployeeCashShortfall record = EmployeeCashShortfall.builder()
                .employee(handover.getEmployee())
                .shiftHandover(handover)
                .workShift(handover.getWorkShift())
                .recordDate(LocalDate.now(GYM_ZONE))
                .expectedAmount(expectedAmount)
                .declaredAmount(declaredAmount)
                .shortfallAmount(shortfall)
                .status(CashShortfallStatus.PENDING)
                .notes("Registrado al entregar turno " + handover.getWorkShift().getName())
                .build();
        return Optional.of(toResponse(shortfallRepository.save(record)));
    }

    @Transactional(readOnly = true)
    public Optional<CashShortfallResponse> findByHandoverId(Long shiftHandoverId) {
        if (shiftHandoverId == null) {
            return Optional.empty();
        }
        return shortfallRepository.findByShiftHandoverId(shiftHandoverId).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CashShortfallResponse> findForMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        if (SecurityUtils.isAdmin()) {
            return shortfallRepository.findByRecordDateBetween(start, end).stream()
                    .map(this::toResponse)
                    .toList();
        }
        Long employeeId = requireCurrentEmployeeId();
        return shortfallRepository
                .findByEmployeeAndRecordDateBetween(employeeId, start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CashShortfallMonthlySummaryResponse> monthlySummary(int year, int month) {
        if (!SecurityUtils.isAdmin()) {
            throw new BusinessException("Solo administración puede ver el resumen por empleado");
        }
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return shortfallRepository
                .summarizeByEmployeeForPeriod(start, end, CashShortfallStatus.PENDING, CashShortfallStatus.SETTLED)
                .stream()
                .map(row -> new CashShortfallMonthlySummaryResponse(
                        (Long) row[0],
                        row[1] + " " + row[2],
                        (BigDecimal) row[3],
                        (BigDecimal) row[4],
                        ((Number) row[5]).longValue()))
                .toList();
    }

    @Transactional
    public CashShortfallResponse settle(Long id, SettleCashShortfallRequest request) {
        if (!SecurityUtils.isAdmin()) {
            throw new BusinessException("Solo administración puede marcar descuadres como cobrados");
        }
        EmployeeCashShortfall record = shortfallRepository
                .findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Descuadre no encontrado: " + id));
        if (record.getStatus() == CashShortfallStatus.SETTLED) {
            throw new BusinessException("Este descuadre ya fue marcado como cobrado");
        }
        record.setStatus(CashShortfallStatus.SETTLED);
        record.setSettledAt(Instant.now());
        record.setSettledBy(employeeService.getEmployee(requireCurrentEmployeeId()));
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            record.setNotes(request.notes().trim());
        }
        return toResponse(shortfallRepository.save(record));
    }

    private CashShortfallResponse toResponse(EmployeeCashShortfall record) {
        Employee employee = record.getEmployee();
        String settledByName = null;
        if (record.getSettledBy() != null) {
            settledByName = record.getSettledBy().getFirstName() + " " + record.getSettledBy().getLastName();
        }
        return new CashShortfallResponse(
                record.getId(),
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                record.getWorkShift().getId(),
                record.getWorkShift().getName(),
                record.getShiftHandover() != null ? record.getShiftHandover().getId() : null,
                record.getRecordDate(),
                record.getExpectedAmount(),
                record.getDeclaredAmount(),
                record.getShortfallAmount(),
                record.getStatus(),
                statusLabel(record.getStatus()),
                record.getNotes(),
                record.getSettledAt(),
                settledByName,
                record.getCreatedAt());
    }

    private static String statusLabel(CashShortfallStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente de cobro";
            case SETTLED -> "Cobrado";
        };
    }

    private Long requireCurrentEmployeeId() {
        Long id = SecurityUtils.currentEmployeeId();
        if (id == null) {
            throw new BusinessException("Tu usuario no está vinculado a un empleado");
        }
        return id;
    }
}
