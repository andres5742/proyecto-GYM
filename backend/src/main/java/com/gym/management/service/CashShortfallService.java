package com.gym.management.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.dto.CashShortfallMonthlySummaryResponse;
import com.gym.management.dto.CashShortfallResponse;
import com.gym.management.dto.InventoryMissingLineDto;
import com.gym.management.dto.SettleCashShortfallRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.CashShortfallKind;
import com.gym.management.model.CashShortfallStatus;
import com.gym.management.model.BillingCashRegister;
import com.gym.management.model.Employee;
import com.gym.management.model.EmployeeCashShortfall;
import com.gym.management.model.ShiftHandover;
import com.gym.management.model.WorkShift;
import com.gym.management.repository.EmployeeCashShortfallRepository;
import com.gym.management.security.SecurityUtils;
import com.gym.management.util.MoneyUtil;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CashShortfallService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");
    private static final TypeReference<List<InventoryMissingLineDto>> INVENTORY_LINES_TYPE =
            new TypeReference<>() {};

    private final EmployeeCashShortfallRepository shortfallRepository;
    private final EmployeeService employeeService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CashShortfallResponse registerFromInventoryCheck(
            WorkShift previousShift, BigDecimal shortfallAmount, List<InventoryMissingLineDto> missingLines) {
        BigDecimal amount = MoneyUtil.roundPesos(shortfallAmount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("No hay faltante de inventario para registrar");
        }
        if (missingLines == null || missingLines.isEmpty()) {
            throw new BusinessException("Debe indicar los productos faltantes");
        }
        String json = writeInventoryJson(missingLines);
        String notes = buildInventoryNotes(missingLines);
        EmployeeCashShortfall record = EmployeeCashShortfall.builder()
                .employee(previousShift.getEmployee())
                .workShift(previousShift)
                .shiftHandover(null)
                .recordDate(LocalDate.now(GYM_ZONE))
                .expectedAmount(amount)
                .declaredAmount(BigDecimal.ZERO)
                .shortfallAmount(amount)
                .status(CashShortfallStatus.PENDING)
                .kind(CashShortfallKind.INVENTORY)
                .notes(notes)
                .inventoryMissingJson(json)
                .build();
        return toResponse(shortfallRepository.save(record));
    }

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
        BigDecimal expected = MoneyUtil.roundPesos(expectedAmount);
        BigDecimal declared = MoneyUtil.roundPesos(declaredAmount);
        if (declared.compareTo(expected) >= 0) {
            return Optional.empty();
        }
        BigDecimal shortfall = expected.subtract(declared);
        EmployeeCashShortfall record = EmployeeCashShortfall.builder()
                .employee(handover.getEmployee())
                .shiftHandover(handover)
                .workShift(handover.getWorkShift())
                .recordDate(LocalDate.now(GYM_ZONE))
                .expectedAmount(expected)
                .declaredAmount(declared)
                .shortfallAmount(shortfall)
                .status(CashShortfallStatus.PENDING)
                .kind(CashShortfallKind.CASH_HANDOVER)
                .notes("Registrado al entregar turno " + handover.getWorkShift().getName())
                .build();
        return Optional.of(toResponse(shortfallRepository.save(record)));
    }

    @Transactional
    public Optional<CashShortfallResponse> registerFromCashRegisterClose(
            BillingCashRegister register,
            Employee responsible,
            WorkShift referenceShift,
            BigDecimal expectedAmount,
            BigDecimal declaredAmount) {
        if (register.getId() == null) {
            return Optional.empty();
        }
        if (shortfallRepository.existsByBillingCashRegisterId(register.getId())) {
            return shortfallRepository
                    .findByBillingCashRegisterIdAndKind(register.getId(), CashShortfallKind.CASH_REGISTER)
                    .map(this::toResponse);
        }
        BigDecimal expected = MoneyUtil.roundPesos(expectedAmount);
        BigDecimal declared = MoneyUtil.roundPesos(declaredAmount);
        if (declared.compareTo(expected) >= 0) {
            return Optional.empty();
        }
        BigDecimal shortfall = expected.subtract(declared);
        String notes =
                "Falta de dinero en caja al cerrar: se esperaban "
                        + MoneyUtil.formatPesos(expected)
                        + ", se contaron "
                        + MoneyUtil.formatPesos(declared)
                        + ".";
        EmployeeCashShortfall record = EmployeeCashShortfall.builder()
                .employee(responsible)
                .billingCashRegister(register)
                .shiftHandover(null)
                .workShift(referenceShift)
                .recordDate(register.getRegisterDate())
                .expectedAmount(expected)
                .declaredAmount(declared)
                .shortfallAmount(shortfall)
                .status(CashShortfallStatus.PENDING)
                .kind(CashShortfallKind.CASH_REGISTER)
                .notes(notes)
                .build();
        return Optional.of(toResponse(shortfallRepository.save(record)));
    }

    @Transactional
    public Optional<CashShortfallResponse> registerFromCashRegisterInventory(
            Employee responsible,
            WorkShift referenceShift,
            LocalDate recordDate,
            BigDecimal shortfallAmount,
            List<InventoryMissingLineDto> missingLines) {
        BigDecimal amount = MoneyUtil.roundPesos(shortfallAmount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        if (missingLines == null || missingLines.isEmpty()) {
            throw new BusinessException("Debe indicar los productos faltantes");
        }
        String json = writeInventoryJson(missingLines);
        String notes = buildCashRegisterInventoryNotes(missingLines);
        EmployeeCashShortfall record = EmployeeCashShortfall.builder()
                .employee(responsible)
                .shiftHandover(null)
                .billingCashRegister(null)
                .workShift(referenceShift)
                .recordDate(recordDate)
                .expectedAmount(amount)
                .declaredAmount(BigDecimal.ZERO)
                .shortfallAmount(amount)
                .status(CashShortfallStatus.PENDING)
                .kind(CashShortfallKind.INVENTORY)
                .notes(notes)
                .inventoryMissingJson(json)
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
            String adminNote = request.notes().trim();
            if (record.getKind() == CashShortfallKind.INVENTORY && record.getNotes() != null) {
                record.setNotes(record.getNotes() + " · Cobro: " + adminNote);
            } else {
                record.setNotes(adminNote);
            }
        }
        return toResponse(shortfallRepository.save(record));
    }

    private CashShortfallResponse toResponse(EmployeeCashShortfall record) {
        Employee employee = record.getEmployee();
        String settledByName = null;
        if (record.getSettledBy() != null) {
            settledByName = record.getSettledBy().getFirstName() + " " + record.getSettledBy().getLastName();
        }
        CashShortfallKind kind = resolveKind(record);
        List<InventoryMissingLineDto> inventoryLines = parseInventoryJson(record.getInventoryMissingJson());
        return new CashShortfallResponse(
                record.getId(),
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                record.getWorkShift().getId(),
                record.getWorkShift().getName(),
                record.getShiftHandover() != null ? record.getShiftHandover().getId() : null,
                record.getRecordDate(),
                MoneyUtil.roundPesos(record.getExpectedAmount()),
                MoneyUtil.roundPesos(record.getDeclaredAmount()),
                MoneyUtil.roundPesos(record.getShortfallAmount()),
                record.getStatus(),
                statusLabel(record.getStatus()),
                kind,
                kindLabel(kind),
                record.getNotes(),
                inventoryLines,
                record.getSettledAt(),
                settledByName,
                record.getCreatedAt());
    }

    private static CashShortfallKind resolveKind(EmployeeCashShortfall record) {
        if (record.getKind() != null) {
            return record.getKind();
        }
        if (record.getBillingCashRegister() != null) {
            return CashShortfallKind.CASH_REGISTER;
        }
        if (record.getShiftHandover() != null) {
            return CashShortfallKind.CASH_HANDOVER;
        }
        if (record.getInventoryMissingJson() != null && !record.getInventoryMissingJson().isBlank()) {
            return CashShortfallKind.INVENTORY;
        }
        String notes = record.getNotes();
        if (notes != null && notes.toLowerCase().contains("inventario")) {
            return CashShortfallKind.INVENTORY;
        }
        return CashShortfallKind.CASH_HANDOVER;
    }

    private static String kindLabel(CashShortfallKind kind) {
        return switch (kind) {
            case INVENTORY -> "Inventario";
            case CASH_HANDOVER -> "Caja (entrega)";
            case CASH_REGISTER -> "Caja (cierre)";
        };
    }

    private static String statusLabel(CashShortfallStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente de cobro";
            case SETTLED -> "Cobrado";
        };
    }

    static String buildInventoryNotes(List<InventoryMissingLineDto> lines) {
        int products = lines.size();
        int units = lines.stream().mapToInt(InventoryMissingLineDto::missingQuantity).sum();
        return "Faltante de inventario al abrir turno: "
                + products
                + " producto(s), "
                + units
                + " unidad(es) faltantes en total.";
    }

    static String buildCashRegisterInventoryNotes(List<InventoryMissingLineDto> lines) {
        int products = lines.size();
        int units = lines.stream().mapToInt(InventoryMissingLineDto::missingQuantity).sum();
        return "Faltante de inventario al cerrar caja: "
                + products
                + " producto(s), "
                + units
                + " unidad(es) faltantes en total.";
    }

    private String writeInventoryJson(List<InventoryMissingLineDto> lines) {
        try {
            return objectMapper.writeValueAsString(lines);
        } catch (Exception e) {
            throw new BusinessException("No se pudo guardar el detalle de inventario");
        }
    }

    private List<InventoryMissingLineDto> parseInventoryJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, INVENTORY_LINES_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    private Long requireCurrentEmployeeId() {
        Long id = SecurityUtils.currentEmployeeId();
        if (id == null) {
            throw new BusinessException("Tu usuario no está vinculado a un empleado");
        }
        return id;
    }
}
