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
import com.gym.management.model.EmployeeCashShortfall;
import com.gym.management.repository.EmployeeCashShortfallRepository;
import com.gym.management.repository.SaleRepository;
import com.gym.management.repository.ShiftHandoverRepository;
import com.gym.management.repository.WorkShiftRepository;
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
    private final BillingCashRegisterService billingCashRegisterService;
    private final WorkShiftRepository workShiftRepository;
    private final SaleRepository saleRepository;
    private final CashShortfallService cashShortfallService;
    private final EmployeeCashShortfallRepository shortfallRepository;
    private final ProductCreditService productCreditService;

    private record ExpectedCashTotals(
            BigDecimal billingCashInDrawer,
            BigDecimal previousShiftSalesCash,
            BigDecimal previousShiftShortfallsDeducted,
            BigDecimal previousShiftCreditPaymentsCash,
            String previousShiftName,
            BigDecimal handoverShiftSalesCash,
            BigDecimal creditPaymentsCash,
            BigDecimal total) {}

    @Transactional(readOnly = true)
    public List<ShiftHandoverResponse> findAll() {
        List<ShiftHandover> list;
        if (SecurityUtils.isAdmin()) {
            list = handoverRepository.findAllByOrderBySubmittedAtDesc();
        } else {
            Long employeeId = requireUser().employeeId();
            if (employeeId == null) {
                list = List.of();
            } else {
                list = handoverRepository.findByEmployeeIdOrderBySubmittedAtDesc(employeeId);
            }
        }
        return list.stream()
                .map(h -> toSummaryResponse(h, java.util.Optional.empty()))
                .toList();
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
        // El descuadre de caja referencia esta entrega; hay que quitarlo antes del DELETE.
        shortfallRepository.findByShiftHandoverId(id).ifPresent(shortfallRepository::delete);
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
                .auxAmount(BigDecimal.ZERO)
                .nequiAmount(BigDecimal.ZERO)
                .bankAmount(BigDecimal.ZERO)
                .notes(request.notes())
                .submittedAt(Instant.now())
                .build();

        applyExpenses(handover, request.expenses());
        applyPriorPayments(handover, request.priorPayments());

        ShiftHandover saved = handoverRepository.save(handover);

        if (shift.getStatus() == ShiftStatus.OPEN) {
            workShiftService.close(shift.getId());
        }

        ShiftDetailResponse detail = saleService.getShiftDetail(shift.getId());
        ExpectedCashTotals expected = computeExpectedCash(shift.getId(), detail.summary());
        BigDecimal declared = CashCountUtil.totalCash(saved);
        java.util.Optional<com.gym.management.dto.CashShortfallResponse> shortfall =
                cashShortfallService.registerFromHandover(saved, expected.total(), declared);

        return toSummaryResponse(saved, shortfall);
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
            if (item.paymentMethod() == PaymentMethod.PENDING
                    || item.paymentMethod() == PaymentMethod.AUX) {
                throw new BusinessException(
                        "En cobros de deudas anteriores use efectivo, Nequi o Bancolombia");
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

    private ShiftHandoverResponse toSummaryResponse(
            ShiftHandover handover, java.util.Optional<com.gym.management.dto.CashShortfallResponse> shortfall) {
        BigDecimal expensesTotal = sumExpenses(handover);
        BigDecimal priorTotal = sumPriorPayments(handover);
        ShiftDetailResponse detail = saleService.getShiftDetail(handover.getWorkShift().getId());
        ExpectedCashTotals expected = computeExpectedCash(handover.getWorkShift().getId(), detail.summary());
        java.math.BigDecimal registeredShortfall = shortfall
                .map(com.gym.management.dto.CashShortfallResponse::shortfallAmount)
                .orElse(null);
        Long shortfallId = shortfall.map(com.gym.management.dto.CashShortfallResponse::id).orElse(null);
        if (registeredShortfall == null && handover.getId() != null) {
            java.util.Optional<com.gym.management.dto.CashShortfallResponse> existing =
                    cashShortfallService.findByHandoverId(handover.getId());
            registeredShortfall =
                    existing.map(com.gym.management.dto.CashShortfallResponse::shortfallAmount).orElse(null);
            shortfallId = existing.map(com.gym.management.dto.CashShortfallResponse::id).orElse(null);
        }
        return ShiftHandoverMapper.toResponse(
                handover,
                expensesTotal,
                priorTotal,
                detail,
                expected.billingCashInDrawer(),
                expected.previousShiftSalesCash(),
                expected.previousShiftShortfallsDeducted(),
                expected.previousShiftName(),
                expected.handoverShiftSalesCash(),
                expected.previousShiftCreditPaymentsCash(),
                expected.creditPaymentsCash(),
                expected.total(),
                buildComparisons(handover, detail.summary(), expected),
                registeredShortfall,
                shortfallId);
    }

    private ShiftHandoverResponse toFullResponse(ShiftHandover handover) {
        ShiftHandover loaded = getHandoverWithDetails(handover.getId());
        return toSummaryResponse(loaded, java.util.Optional.empty());
    }

    private ShiftHandoverResponse buildPreviewResponse(WorkShift shift) {
        ShiftDetailResponse detail = saleService.getShiftDetail(shift.getId());
        ShiftHandover empty = ShiftHandover.builder()
                .workShift(shift)
                .employee(shift.getEmployee())
                .submittedAt(Instant.now())
                .build();
        ExpectedCashTotals expected = computeExpectedCash(shift.getId(), detail.summary());
        return ShiftHandoverMapper.toResponse(
                empty,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                detail,
                expected.billingCashInDrawer(),
                expected.previousShiftSalesCash(),
                expected.previousShiftShortfallsDeducted(),
                expected.previousShiftName(),
                expected.handoverShiftSalesCash(),
                expected.previousShiftCreditPaymentsCash(),
                expected.creditPaymentsCash(),
                expected.total(),
                buildComparisons(empty, detail.summary(), expected),
                null,
                null);
    }

    private ExpectedCashTotals computeExpectedCash(Long handoverShiftId, SalesSummaryResponse sales) {
        BigDecimal billing = billingCashRegisterService.cashInDrawerForToday();
        BigDecimal handoverShiftCash =
                sales.amountByPaymentMethod().getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        BigDecimal creditCash = productCreditService.sumCashPaymentsForShift(handoverShiftId);
        PreviousShiftCash previous = resolvePreviousShiftCash(handoverShiftId);
        BigDecimal total = billing
                .add(previous.netSalesCash())
                .add(previous.creditPaymentsCash())
                .add(handoverShiftCash)
                .add(creditCash);
        return new ExpectedCashTotals(
                billing,
                previous.netSalesCash(),
                previous.shortfallsDeducted(),
                previous.creditPaymentsCash(),
                previous.name(),
                handoverShiftCash,
                creditCash,
                total);
    }

    private record PreviousShiftCash(
            BigDecimal netSalesCash, BigDecimal shortfallsDeducted, BigDecimal creditPaymentsCash, String name) {}

    private PreviousShiftCash resolvePreviousShiftCash(Long handoverShiftId) {
        WorkShift current = workShiftService.getShift(handoverShiftId);
        List<WorkShift> previousShifts = workShiftRepository.findByShiftDateAndStatusAndOpenedAtBeforeOrderByOpenedAtAsc(
                current.getShiftDate(), ShiftStatus.CLOSED, current.getOpenedAt());
        if (previousShifts.isEmpty()) {
            return new PreviousShiftCash(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        }
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal deductedTotal = BigDecimal.ZERO;
        BigDecimal creditCashTotal = BigDecimal.ZERO;
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < previousShifts.size(); i++) {
            WorkShift shift = previousShifts.get(i);
            BigDecimal cash = saleRepository.sumTotalByPaymentMethodAndShift(PaymentMethod.CASH, shift.getId());
            if (cash == null) {
                cash = BigDecimal.ZERO;
            }
            BigDecimal shortfall = shortfallRepository
                    .findByWorkShiftId(shift.getId())
                    .map(EmployeeCashShortfall::getShortfallAmount)
                    .orElse(BigDecimal.ZERO);
            deductedTotal = deductedTotal.add(shortfall);
            BigDecimal net = cash.subtract(shortfall);
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                netTotal = netTotal.add(net);
            }
            BigDecimal shiftCreditCash = productCreditService.sumCashPaymentsForShift(shift.getId());
            creditCashTotal = creditCashTotal.add(shiftCreditCash);
            if (i > 0) {
                names.append(", ");
            }
            names.append(shift.getName());
        }
        return new PreviousShiftCash(netTotal, deductedTotal, creditCashTotal, names.toString());
    }

    private List<ShiftHandoverComparisonResponse> buildComparisons(
            ShiftHandover handover, SalesSummaryResponse sales, ExpectedCashTotals expected) {
        Map<PaymentMethod, BigDecimal> byMethod = sales.amountByPaymentMethod();
        BigDecimal expectedPending = byMethod.getOrDefault(PaymentMethod.PENDING, BigDecimal.ZERO);

        BigDecimal cashCounted = CashCountUtil.totalCash(handover);
        BigDecimal priorTotal = sumPriorPayments(handover);

        List<ShiftHandoverComparisonResponse> list = new ArrayList<>();
        list.add(comparison(
                "Dinero contado (billetes + monedas)",
                cashCounted,
                expected.total()));
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
        AuthenticatedUser user = requireUser();
        Long employeeId = user.employeeId();
        if (employeeId == null) {
            throw new BusinessException(
                    "Tu usuario no está vinculado a un empleado. Pide al administrador que lo asocie.");
        }
        if (shift.getStatus() == ShiftStatus.OPEN && workShiftService.isGlobalOpenShift(shift.getId())) {
            return;
        }
        if (shift.getEmployee() != null && shift.getEmployee().getId().equals(employeeId)) {
            return;
        }
        throw new BusinessException(
                "No puedes gestionar este turno. Solo el turno abierto del gimnasio o los tuyos.");
    }

    private AuthenticatedUser requireUser() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user == null) {
            throw new BusinessException("Sesión no válida");
        }
        return user;
    }
}
