package com.gym.management.service;

import com.gym.management.dto.ShiftDetailResponse;
import com.gym.management.dto.ShiftHandoverComparisonResponse;
import com.gym.management.dto.ShiftHandoverExpenseRequest;
import com.gym.management.dto.ShiftHandoverPriorPaymentRequest;
import com.gym.management.dto.ShiftHandoverRequest;
import com.gym.management.dto.ShiftHandoverResponse;
import com.gym.management.dto.SalesSummaryResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.ShiftHandoverMapper;
import com.gym.management.model.PaymentMethod;
import com.gym.management.model.ShiftHandover;
import com.gym.management.model.ShiftHandoverExpense;
import com.gym.management.model.ShiftHandoverPriorPayment;
import com.gym.management.model.ShiftStatus;
import com.gym.management.model.WorkShift;
import com.gym.management.repository.ShiftHandoverRepository;
import com.gym.management.security.AuthenticatedUser;
import com.gym.management.security.SecurityUtils;
import com.gym.management.util.CashCountUtil;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftHandoverService {

    private final ShiftHandoverRepository handoverRepository;
    private final WorkShiftService workShiftService;
    private final SaleService saleService;

    @Transactional(readOnly = true)
    public List<ShiftHandoverResponse> findAll() {
        List<ShiftHandover> list = SecurityUtils.isAdmin()
                ? handoverRepository.findAllByOrderBySubmittedAtDesc()
                : handoverRepository.findByEmployeeIdOrderBySubmittedAtDesc(requireUser().employeeId());
        return list.stream().map(this::toSummaryResponse).toList();
    }

    @Transactional(readOnly = true)
    public ShiftHandoverResponse findById(Long id) {
        ShiftHandover handover = getHandoverWithDetails(id);
        ensureCanView(handover);
        return toFullResponse(handover);
    }

    @Transactional(readOnly = true)
    public ShiftHandoverResponse previewForShift(Long workShiftId) {
        WorkShift shift = workShiftService.getShift(workShiftId);
        ensureCanManageShift(shift);
        if (handoverRepository.existsByWorkShiftId(workShiftId)) {
            return toFullResponse(getHandoverWithDetails(
                    handoverRepository.findByWorkShiftId(workShiftId).orElseThrow().getId()));
        }
        return buildPreviewResponse(shift);
    }

    @Transactional
    public void delete(Long id) {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException("Solo el super administrador puede eliminar entregas de turno");
        }
        ShiftHandover handover = handoverRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega de turno no encontrada: " + id));
        handoverRepository.delete(handover);
    }

    @Transactional
    public ShiftHandoverResponse submit(ShiftHandoverRequest request) {
        WorkShift shift = workShiftService.getShift(request.workShiftId());
        ensureCanManageShift(shift);
        if (handoverRepository.existsByWorkShiftId(shift.getId())) {
            throw new BusinessException("Este turno ya tiene una entrega registrada");
        }

        ShiftHandover handover = ShiftHandover.builder()
                .workShift(shift)
                .employee(shift.getEmployee())
                .bill2000(request.bill2000())
                .bill5000(request.bill5000())
                .bill10000(request.bill10000())
                .bill20000(request.bill20000())
                .bill50000(request.bill50000())
                .bill100000(request.bill100000())
                .coin1000(request.coin1000())
                .coin500(request.coin500())
                .coin200(request.coin200())
                .coin100(request.coin100())
                .coin50(request.coin50())
                .auxAmount(request.auxAmount())
                .nequiAmount(request.nequiAmount())
                .bankAmount(request.bankAmount())
                .notes(request.notes())
                .submittedAt(Instant.now())
                .build();

        applyExpenses(handover, request.expenses());
        applyPriorPayments(handover, request.priorPayments());

        ShiftHandover saved = handoverRepository.save(handover);

        if (shift.getStatus() == ShiftStatus.OPEN) {
            workShiftService.close(shift.getId());
        }

        return toFullResponse(saved);
    }

    private void applyExpenses(ShiftHandover handover, List<ShiftHandoverExpenseRequest> expenses) {
        if (expenses == null) {
            return;
        }
        for (ShiftHandoverExpenseRequest item : expenses) {
            if (item == null || item.description() == null || item.description().isBlank()) {
                continue;
            }
            handover.getExpenses()
                    .add(ShiftHandoverExpense.builder()
                            .handover(handover)
                            .description(item.description().trim())
                            .amount(item.amount())
                            .build());
        }
    }

    private void applyPriorPayments(ShiftHandover handover, List<ShiftHandoverPriorPaymentRequest> payments) {
        if (payments == null) {
            return;
        }
        for (ShiftHandoverPriorPaymentRequest item : payments) {
            if (item == null || item.description() == null || item.description().isBlank()) {
                continue;
            }
            if (item.paymentMethod() == PaymentMethod.PENDING) {
                throw new BusinessException(
                        "En cobros de deudas anteriores use efectivo, Nequi, Bancolombia o AUX");
            }
            handover.getPriorPayments()
                    .add(ShiftHandoverPriorPayment.builder()
                            .handover(handover)
                            .description(item.description().trim())
                            .amount(item.amount())
                            .paymentMethod(item.paymentMethod())
                            .notes(item.notes())
                            .build());
        }
    }

    private ShiftHandoverResponse toSummaryResponse(ShiftHandover handover) {
        BigDecimal expensesTotal = sumExpenses(handover);
        BigDecimal priorTotal = sumPriorPayments(handover);
        ShiftDetailResponse detail = saleService.getShiftDetail(handover.getWorkShift().getId());
        return ShiftHandoverMapper.toResponse(
                handover, expensesTotal, priorTotal, detail, buildComparisons(handover, detail.summary()));
    }

    private ShiftHandoverResponse toFullResponse(ShiftHandover handover) {
        ShiftHandover loaded = getHandoverWithDetails(handover.getId());
        return toSummaryResponse(loaded);
    }

    private ShiftHandoverResponse buildPreviewResponse(WorkShift shift) {
        ShiftDetailResponse detail = saleService.getShiftDetail(shift.getId());
        ShiftHandover empty = ShiftHandover.builder()
                .workShift(shift)
                .employee(shift.getEmployee())
                .submittedAt(Instant.now())
                .build();
        return ShiftHandoverMapper.toResponse(empty, BigDecimal.ZERO, BigDecimal.ZERO, detail, buildComparisons(empty, detail.summary()));
    }

    private List<ShiftHandoverComparisonResponse> buildComparisons(
            ShiftHandover handover, SalesSummaryResponse sales) {
        Map<PaymentMethod, BigDecimal> byMethod = sales.amountByPaymentMethod();
        BigDecimal salesCash = byMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        BigDecimal salesAux = byMethod.getOrDefault(PaymentMethod.AUX, BigDecimal.ZERO);
        BigDecimal expectedNequi = byMethod.getOrDefault(PaymentMethod.NEQUI, BigDecimal.ZERO);
        BigDecimal expectedBank = byMethod.getOrDefault(PaymentMethod.BANCOLOMBIA, BigDecimal.ZERO);
        BigDecimal expectedPending = byMethod.getOrDefault(PaymentMethod.PENDING, BigDecimal.ZERO);

        BigDecimal cashCounted = CashCountUtil.totalCash(handover);
        BigDecimal auxDeclared = handover.getAuxAmount() != null ? handover.getAuxAmount() : BigDecimal.ZERO;
        BigDecimal expectedCashInDrawer = salesCash.add(auxDeclared);
        BigDecimal priorTotal = sumPriorPayments(handover);

        List<ShiftHandoverComparisonResponse> list = new ArrayList<>();
        list.add(comparison(
                "Dinero contado (billetes + monedas)",
                cashCounted,
                expectedCashInDrawer));
        list.add(comparison("Monto AUX declarado", auxDeclared, salesAux));
        list.add(comparison("Nequi declarado", handover.getNequiAmount(), expectedNequi));
        list.add(comparison("Bancolombia declarado", handover.getBankAmount(), expectedBank));
        list.add(comparison("Cobros deudas anteriores", priorTotal, expectedPending));
        return list;
    }

    private ShiftHandoverComparisonResponse comparison(String label, BigDecimal declared, BigDecimal expected) {
        return new ShiftHandoverComparisonResponse(label, declared, expected, declared.subtract(expected));
    }

    private BigDecimal sumExpenses(ShiftHandover handover) {
        return handover.getExpenses().stream()
                .map(ShiftHandoverExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumPriorPayments(ShiftHandover handover) {
        return handover.getPriorPayments().stream()
                .map(ShiftHandoverPriorPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ShiftHandover getHandoverWithDetails(Long id) {
        ShiftHandover handover = handoverRepository
                .findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega de turno no encontrada: " + id));
        handover.getExpenses().size();
        handover.getPriorPayments().size();
        return handover;
    }

    private void ensureCanView(ShiftHandover handover) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        if (!handover.getEmployee().getId().equals(requireUser().employeeId())) {
            throw new BusinessException("No tienes permiso para ver esta entrega");
        }
    }

    private void ensureCanManageShift(WorkShift shift) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        if (shift.getEmployee() == null
                || !shift.getEmployee().getId().equals(requireUser().employeeId())) {
            throw new BusinessException("Solo puedes entregar tu propio turno");
        }
    }

    private AuthenticatedUser requireUser() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user == null) {
            throw new BusinessException("Sesión no válida");
        }
        return user;
    }
}
