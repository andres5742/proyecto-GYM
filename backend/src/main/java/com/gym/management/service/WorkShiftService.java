package com.gym.management.service;

import com.gym.management.dto.ShiftOpenInventoryPreviewResponse;
import com.gym.management.dto.WorkShiftOpenResultResponse;
import com.gym.management.dto.WorkShiftRequest;
import com.gym.management.dto.WorkShiftResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.WorkShiftMapper;
import com.gym.management.model.Employee;
import com.gym.management.model.Sale;
import com.gym.management.model.ShiftStatus;
import com.gym.management.model.UserRole;
import com.gym.management.model.WorkShift;
import com.gym.management.repository.SaleRepository;
import com.gym.management.repository.WorkShiftRepository;
import com.gym.management.security.AuthenticatedUser;
import com.gym.management.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkShiftService {

    private final WorkShiftRepository workShiftRepository;
    private final SaleRepository saleRepository;
    private final EmployeeService employeeService;
    private final ShiftInventoryService shiftInventoryService;

    @Transactional(readOnly = true)
    public List<WorkShiftResponse> findAll() {
        return workShiftRepository.findAllByOrderByShiftDateDescOpenedAtDesc().stream()
                .map(this::toResponseWithTotals)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkShiftResponse findOpen() {
        return workShiftRepository.findFirstByStatusOrderByOpenedAtDesc(ShiftStatus.OPEN)
                .map(this::toResponseWithTotals)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public WorkShiftResponse findById(Long id) {
        return toResponseWithTotals(getShift(id));
    }

    @Transactional(readOnly = true)
    public ShiftOpenInventoryPreviewResponse openInventoryPreview(LocalDate shiftDate) {
        return shiftInventoryService.preview(shiftDate);
    }

    @Transactional
    public WorkShiftOpenResultResponse open(WorkShiftRequest request) {
        if (workShiftRepository.existsByStatus(ShiftStatus.OPEN)) {
            throw new BusinessException("Ya hay un turno abierto. Ciérrelo antes de abrir otro.");
        }
        Employee seller = resolveSellerForOpen(request.employeeId());
        LocalDate shiftDate = request.shiftDate() != null ? request.shiftDate() : LocalDate.now();
        ShiftInventoryService.InventoryOpenResult inventoryResult =
                shiftInventoryService.processBeforeOpen(shiftDate, request.inventoryCounts(), request.cashCount());
        WorkShift shift = WorkShift.builder()
                .shiftDate(shiftDate)
                .name(request.name())
                .employee(seller)
                .openedAt(LocalDateTime.now())
                .status(ShiftStatus.OPEN)
                .build();
        WorkShiftResponse response =
                WorkShiftMapper.toResponse(workShiftRepository.save(shift), BigDecimal.ZERO, 0L);
        var inventoryShortfall = inventoryResult.inventoryShortfall();
        var cashShortfall = inventoryResult.cashShortfall();
        return new WorkShiftOpenResultResponse(
                response,
                inventoryResult.adjusted(),
                inventoryShortfall != null,
                inventoryResult.inventoryShortfallAmount(),
                inventoryShortfall != null ? inventoryShortfall.notes() : null,
                cashShortfall != null,
                cashShortfall != null ? cashShortfall.shortfallAmount() : java.math.BigDecimal.ZERO,
                cashShortfall != null ? cashShortfall.notes() : null);
    }

    @Transactional
    public WorkShiftResponse close(Long id) {
        WorkShift shift = getShift(id);
        if (shift.getStatus() == ShiftStatus.CLOSED) {
            throw new BusinessException("El turno ya está cerrado");
        }
        shift.setStatus(ShiftStatus.CLOSED);
        shift.setClosedAt(LocalDateTime.now());
        return toResponseWithTotals(workShiftRepository.save(shift));
    }

    public WorkShift getShift(Long id) {
        return workShiftRepository.findWithEmployeeById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + id));
    }

    public WorkShift getOpenShiftRequired() {
        return workShiftRepository.findFirstByStatusOrderByOpenedAtDesc(ShiftStatus.OPEN)
                .orElseThrow(() -> new BusinessException(
                        "No hay un turno abierto. Abra un turno antes de registrar ventas."));
    }

    @Transactional(readOnly = true)
    public boolean isGlobalOpenShift(Long shiftId) {
        if (shiftId == null) {
            return false;
        }
        return workShiftRepository
                .findFirstByStatusOrderByOpenedAtDesc(ShiftStatus.OPEN)
                .map(open -> open.getId().equals(shiftId))
                .orElse(false);
    }

    public Employee getShiftSeller(WorkShift shift) {
        return shift.getEmployee();
    }

    private Employee resolveSellerForOpen(Long requestedEmployeeId) {
        AuthenticatedUser current = SecurityUtils.currentUser();
        if (current == null) {
            throw new BusinessException("Sesión no válida");
        }
        if (requestedEmployeeId != null
                && (current.role() == UserRole.ADMIN || current.role() == UserRole.SUPER_ADMIN)) {
            return employeeService.getEmployee(requestedEmployeeId);
        }
        return employeeService.getEmployee(current.employeeId());
    }

    @Transactional
    public void delete(Long id) {
        WorkShift shift = getShift(id);
        if (shift.getStatus() == ShiftStatus.OPEN) {
            throw new BusinessException("Cierra el turno antes de eliminarlo del historial");
        }
        List<Sale> sales = saleRepository.findByWorkShiftIdOrderBySaleDateDescCreatedAtDesc(id);
        for (Sale sale : sales) {
            var product = sale.getProduct();
            product.setQuantity(product.getQuantity() + sale.getQuantity());
            saleRepository.delete(sale);
        }
        workShiftRepository.delete(shift);
    }

    private WorkShiftResponse toResponseWithTotals(WorkShift shift) {
        BigDecimal total = saleRepository.sumTotalAmountByShift(shift.getId());
        long count = saleRepository.countByShift(shift.getId());
        return WorkShiftMapper.toResponse(shift, total, count);
    }
}
