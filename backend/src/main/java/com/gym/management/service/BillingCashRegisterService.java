package com.gym.management.service;

import com.gym.management.dto.BillingCashRegisterClosePreviewResponse;
import com.gym.management.dto.BillingCashRegisterCloseResultResponse;
import com.gym.management.dto.ShiftOpenCashPreviewResponse;
import com.gym.management.dto.BillingCashRegisterExpenseRequest;
import com.gym.management.dto.BillingCashRegisterExpenseResponse;
import com.gym.management.dto.BillingCashRegisterResponse;
import com.gym.management.dto.CashShortfallResponse;
import com.gym.management.dto.CloseBillingCashRegisterRequest;
import com.gym.management.dto.OpenBillingCashRegisterRequest;
import com.gym.management.model.WorkShift;
import com.gym.management.repository.ProductCreditPaymentRepository;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.BillingCashRegisterExpenseMapper;
import com.gym.management.mapper.BillingCashRegisterMapper;
import com.gym.management.model.BillingCashRegister;
import com.gym.management.model.BillingPaymentType;
import com.gym.management.model.PaymentMethod;
import com.gym.management.model.BillingCashRegisterExpense;
import com.gym.management.model.Employee;
import com.gym.management.model.ShiftStatus;
import com.gym.management.repository.BillingCashRegisterExpenseRepository;
import com.gym.management.repository.BillingCashRegisterRepository;
import com.gym.management.repository.BillingPaymentRepository;
import com.gym.management.repository.SaleRepository;
import com.gym.management.repository.WorkShiftRepository;
import com.gym.management.security.SecurityUtils;
import com.gym.management.util.MoneyUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingCashRegisterService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private final BillingCashRegisterRepository cashRegisterRepository;
    private final BillingPaymentRepository billingPaymentRepository;
    private final SaleRepository saleRepository;
    private final WorkShiftRepository workShiftRepository;
    private final BillingCashRegisterExpenseRepository expenseRepository;
    private final ProductCreditPaymentRepository productCreditPaymentRepository;
    private final EmployeeService employeeService;
    private final CashShortfallService cashShortfallService;

    @Lazy
    @Autowired
    private ShiftInventoryService shiftInventoryService;

    @Transactional(readOnly = true)
    public BillingCashRegisterResponse findOpen() {
        return findTodayOpen().map(this::toResponseWithTotals).orElse(null);
    }

    @Transactional(readOnly = true)
    public BillingCashRegisterResponse findToday() {
        return cashRegisterRepository
                .findFirstByRegisterDateOrderByOpenedAtDesc(today())
                .map(this::toResponseWithTotals)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public ShiftOpenCashPreviewResponse shiftOpenCashPreview(LocalDate shiftDate) {
        LocalDate date = shiftDate != null ? shiftDate : today();
        BillingCashRegister register = cashRegisterRepository
                .findFirstByRegisterDateOrderByOpenedAtDesc(date)
                .orElseThrow(() -> new BusinessException(
                        "No hay caja del día en Facturación. Ábrala antes de abrir otro turno."));
        if (register.getStatus() != ShiftStatus.OPEN) {
            throw new BusinessException(
                    "La caja del día está cerrada. Reábrala en Facturación antes de abrir otro turno.");
        }
        Long id = register.getId();
        Map<PaymentMethod, BigDecimal> expensesByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        expenseRepository.sumByPaymentMethodByCashRegisterId(id));
        BigDecimal cashExpenses = expensesByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        ProductDayTotals productTotals = loadProductTotalsForDate(date);
        Map<BillingPaymentType, BigDecimal> cashByType = loadCashByBillingType(id);
        BigDecimal fiadoCash = sumFiadoCashCollected(date);
        BigDecimal expected = computeCashInDrawer(register, true);
        Employee opener = register.getOpenedBy();
        String openerName = opener.getFirstName() + " " + opener.getLastName();
        return new ShiftOpenCashPreviewResponse(
                id,
                openerName,
                register.getOpeningCashAmount(),
                cashExpenses,
                productTotals.cashTotal(),
                fiadoCash,
                cashByType.getOrDefault(BillingPaymentType.MEMBERSHIP, BigDecimal.ZERO),
                cashByType.getOrDefault(BillingPaymentType.DAY_WORKOUT, BigDecimal.ZERO),
                cashByType.getOrDefault(BillingPaymentType.SPORTS_DANCE, BigDecimal.ZERO),
                expected);
    }

    @Transactional(readOnly = true)
    public List<BillingCashRegisterResponse> findByDate(LocalDate date) {
        LocalDate target = date != null ? date : today();
        return cashRegisterRepository.findAllByRegisterDateOrderByOpenedAtDesc(target).stream()
                .map(this::toResponseWithTotals)
                .toList();
    }

    @Transactional
    public BillingCashRegisterResponse open(OpenBillingCashRegisterRequest request) {
        LocalDate today = today();
        var existingOpt = cashRegisterRepository.findFirstByRegisterDateOrderByOpenedAtDesc(today);
        if (existingOpt.isPresent()) {
            BillingCashRegister existing = existingOpt.get();
            if (existing.getStatus() == ShiftStatus.OPEN) {
                throw new BusinessException("La caja del día ya está abierta. Cualquier usuario autorizado puede facturar.");
            }
            if (SecurityUtils.isSuperAdmin()) {
                return reopenTodayRegister(existing, request);
            }
            throw new BusinessException(
                    "La caja de hoy ya fue cerrada. Solo el super administrador puede volver a abrirla el mismo día.");
        }
        Employee opener = resolveCurrentEmployee();
        BillingCashRegister register = BillingCashRegister.builder()
                .registerDate(today)
                .openedBy(opener)
                .openingCashAmount(request.openingCashAmount())
                .openedAt(LocalDateTime.now(GYM_ZONE))
                .status(ShiftStatus.OPEN)
                .build();
        return BillingCashRegisterMapper.toResponse(
                cashRegisterRepository.save(register),
                BigDecimal.ZERO,
                BillingPaymentMethodTotals.emptyBillableMap(),
                0L,
                BigDecimal.ZERO,
                BillingPaymentMethodTotals.emptyBillableMap(),
                0L,
                BigDecimal.ZERO,
                0L,
                0L,
                0L,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BillingPaymentMethodTotals.emptyBillableMap(),
                0L,
                request.openingCashAmount());
    }

    @Transactional(readOnly = true)
    public List<BillingCashRegisterExpenseResponse> listExpensesByDate(LocalDate date) {
        LocalDate target = date != null ? date : today();
        return expenseRepository.findByRegisterDateWithDetails(target).stream()
                .map(BillingCashRegisterExpenseMapper::toResponse)
                .toList();
    }

    @Transactional
    public BillingCashRegisterExpenseResponse addExpense(BillingCashRegisterExpenseRequest request) {
        BillingCashRegister register = getOpenRegisterRequired();
        Employee recorder = resolveCurrentEmployee();
        PaymentMethod method = request.paymentMethod();
        BillingPaymentMethodRules.requireAllowed(method);
        BillingCashRegisterExpense expense = BillingCashRegisterExpense.builder()
                .cashRegister(register)
                .amount(request.amount())
                .paymentMethod(method)
                .observation(request.observation().trim())
                .recordedBy(recorder)
                .build();
        return BillingCashRegisterExpenseMapper.toResponse(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public BillingCashRegisterClosePreviewResponse closePreview(Long id) {
        assertCanCloseCashRegister();
        assertNoOpenWorkShifts();
        BillingCashRegister register = getRegister(id);
        if (register.getStatus() == ShiftStatus.CLOSED) {
            throw new BusinessException("La caja ya está cerrada");
        }
        BigDecimal cashInDrawer = MoneyUtil.roundPesos(computeCashInDrawer(register, true));
        BigDecimal fiadoCash = MoneyUtil.roundPesos(sumFiadoCashCollected(register.getRegisterDate()));
        return new BillingCashRegisterClosePreviewResponse(
                cashInDrawer,
                fiadoCash,
                cashInDrawer,
                shiftInventoryService.listActiveProductLines());
    }

    @Transactional
    public BillingCashRegisterCloseResultResponse close(Long id, CloseBillingCashRegisterRequest request) {
        assertCanCloseCashRegister();
        assertNoOpenWorkShifts();
        BillingCashRegister register = getRegister(id);
        if (!register.getRegisterDate().equals(today())) {
            throw new BusinessException("Solo se puede cerrar la caja del día actual");
        }
        if (register.getStatus() == ShiftStatus.CLOSED) {
            throw new BusinessException("La caja ya está cerrada");
        }
        Employee closer = resolveCurrentEmployee();
        WorkShift referenceShift = resolveReferenceShift(register.getRegisterDate());

        BigDecimal expectedCash = MoneyUtil.roundPesos(computeCashInDrawer(register, true));
        BigDecimal declaredCash = MoneyUtil.roundPesos(request.cashCount().totalCash());

        ShiftInventoryService.InventoryCloseAtRegisterResult inventoryResult =
                shiftInventoryService.processAtCashRegisterClose(
                        register.getRegisterDate(), request.inventoryCounts(), closer, referenceShift);

        Optional<CashShortfallResponse> cashShortfall = cashShortfallService.registerFromCashRegisterClose(
                register, closer, referenceShift, expectedCash, declaredCash);

        register.setStatus(ShiftStatus.CLOSED);
        register.setClosedAt(LocalDateTime.now(GYM_ZONE));
        BillingCashRegister saved = cashRegisterRepository.save(register);

        return new BillingCashRegisterCloseResultResponse(
                toResponseWithTotals(saved),
                declaredCash,
                expectedCash,
                cashShortfall.orElse(null),
                inventoryResult.shortfall(),
                inventoryResult.missingLines());
    }

    public BillingCashRegister getOpenRegisterRequired() {
        return findTodayOpen()
                .orElseThrow(() -> new BusinessException(
                        "No hay caja abierta hoy. Abra la caja del día antes de registrar pagos o gastos."));
    }

    public BillingCashRegister getRegister(Long id) {
        return cashRegisterRepository
                .findByIdWithEmployee(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caja de facturación no encontrada: " + id));
    }

    private BillingCashRegisterResponse reopenTodayRegister(
            BillingCashRegister register, OpenBillingCashRegisterRequest request) {
        register.setStatus(ShiftStatus.OPEN);
        register.setClosedAt(null);
        register.setOpeningCashAmount(request.openingCashAmount());
        return toResponseWithTotals(cashRegisterRepository.save(register));
    }

    private java.util.Optional<BillingCashRegister> findTodayOpen() {
        return cashRegisterRepository.findFirstByRegisterDateAndStatusOrderByOpenedAtDesc(
                today(), ShiftStatus.OPEN);
    }

    private LocalDate today() {
        return LocalDate.now(GYM_ZONE);
    }

    private Employee resolveCurrentEmployee() {
        Long employeeId = SecurityUtils.currentEmployeeId();
        if (employeeId == null) {
            throw new BusinessException("No se pudo identificar al empleado");
        }
        return employeeService.getEmployee(employeeId);
    }

    private void assertCanCloseCashRegister() {
        if (!SecurityUtils.isAdmin()) {
            throw new BusinessException("No puede cerrar caja. Solo la administración puede cerrar la caja del día.");
        }
    }

    private void assertNoOpenWorkShifts() {
        workShiftRepository
                .findFirstByStatusOrderByOpenedAtDesc(ShiftStatus.OPEN)
                .ifPresent(open -> {
                    throw new BusinessException(
                            "Debe cerrar el turno «" + open.getName() + "» antes de cerrar la caja del día.");
                });
    }

    /**
     * Efectivo físico en caja del día: inicio + facturación en efectivo + ventas de productos en efectivo
     * (todos los turnos) − gastos en efectivo. No incluye Nequi ni otros medios digitales.
     */
    @Transactional(readOnly = true)
    public BigDecimal cashInDrawerForToday() {
        return cashRegisterRepository
                .findFirstByRegisterDateOrderByOpenedAtDesc(today())
                .map(reg -> computeCashInDrawer(reg, false))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Base de caja para entrega de turno: apertura + cobros de facturación en efectivo − gastos en efectivo.
     * Sin ventas de productos ni fiado (esos se suman por turno en {@link ShiftHandoverService}).
     */
    @Transactional(readOnly = true)
    public BigDecimal billingCashExpectedForHandover() {
        return cashRegisterRepository
                .findFirstByRegisterDateOrderByOpenedAtDesc(today())
                .map(this::computeBillingCashForHandover)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal computeBillingCashForHandover(BillingCashRegister register) {
        Long id = register.getId();
        Map<PaymentMethod, BigDecimal> incomeByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        billingPaymentRepository.sumByPaymentMethodByCashRegisterId(id));
        Map<PaymentMethod, BigDecimal> expensesByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        expenseRepository.sumByPaymentMethodByCashRegisterId(id));
        BigDecimal billingCashIn = incomeByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        BigDecimal cashOut = expensesByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        return MoneyUtil.roundPesos(
                register.getOpeningCashAmount().add(billingCashIn).subtract(cashOut));
    }

    private BigDecimal computeCashInDrawer(BillingCashRegister register) {
        return computeCashInDrawer(register, true);
    }

    private BigDecimal computeCashInDrawer(BillingCashRegister register, boolean includeFiadoCash) {
        Long id = register.getId();
        Map<PaymentMethod, BigDecimal> incomeByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        billingPaymentRepository.sumByPaymentMethodByCashRegisterId(id));
        Map<PaymentMethod, BigDecimal> expensesByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        expenseRepository.sumByPaymentMethodByCashRegisterId(id));
        BigDecimal billingCashIn = incomeByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        BigDecimal cashOut = expensesByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        BigDecimal productCash = saleRepository.sumCashAmountByShiftDate(register.getRegisterDate());
        if (productCash == null) {
            productCash = BigDecimal.ZERO;
        }
        BigDecimal total = register.getOpeningCashAmount()
                .add(billingCashIn)
                .add(productCash)
                .subtract(cashOut);
        if (includeFiadoCash) {
            total = total.add(sumFiadoCashCollected(register.getRegisterDate()));
        }
        return MoneyUtil.roundPesos(total);
    }

    private BillingCashRegisterResponse toResponseWithTotals(BillingCashRegister register) {
        Long id = register.getId();
        Map<PaymentMethod, BigDecimal> incomeByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        billingPaymentRepository.sumByPaymentMethodByCashRegisterId(id));
        BigDecimal total = BillingPaymentMethodTotals.sum(incomeByMethod);
        long count = billingPaymentRepository.countByBillingCashRegisterId(id);
        Map<PaymentMethod, BigDecimal> expensesByMethod =
                BillingPaymentMethodTotals.fromAmountRows(expenseRepository.sumByPaymentMethodByCashRegisterId(id));
        BigDecimal expenses = BillingPaymentMethodTotals.sum(expensesByMethod);
        long expenseCount = expenseRepository.countByCashRegisterId(id);
        ProductDayTotals productTotals = loadProductTotalsForDate(register.getRegisterDate());
        long shiftsWithSales = workShiftRepository.countShiftsWithSalesByShiftDate(register.getRegisterDate());
        Map<BillingPaymentType, BigDecimal> cashByType = loadCashByBillingType(id);
        FiadoDayTotals fiadoTotals = loadFiadoTotalsForDate(register.getRegisterDate());
        BigDecimal cashInDrawer = computeCashInDrawer(register, true);
        return BillingCashRegisterMapper.toResponse(
                register,
                total,
                incomeByMethod,
                count,
                expenses,
                expensesByMethod,
                expenseCount,
                productTotals.total(),
                productTotals.saleCount(),
                shiftsWithSales,
                productTotals.units(),
                productTotals.cashTotal(),
                cashByType.getOrDefault(BillingPaymentType.MEMBERSHIP, BigDecimal.ZERO),
                cashByType.getOrDefault(BillingPaymentType.DAY_WORKOUT, BigDecimal.ZERO),
                cashByType.getOrDefault(BillingPaymentType.SPORTS_DANCE, BigDecimal.ZERO),
                fiadoTotals.total(),
                fiadoTotals.byMethod(),
                fiadoTotals.paymentCount(),
                cashInDrawer);
    }

    private FiadoDayTotals loadFiadoTotalsForDate(LocalDate date) {
        BigDecimal total = productCreditPaymentRepository.sumAmountByShiftDate(date);
        Map<PaymentMethod, BigDecimal> byMethod = BillingPaymentMethodTotals.fromAmountRows(
                productCreditPaymentRepository.sumByShiftDateGroupByPaymentMethod(date));
        long paymentCount = productCreditPaymentRepository.countByShiftDate(date);
        return new FiadoDayTotals(total != null ? total : BigDecimal.ZERO, byMethod, paymentCount);
    }

    private ProductDayTotals loadProductTotalsForDate(LocalDate date) {
        BigDecimal total = saleRepository.sumTotalAmountByShiftDate(date);
        long saleCount = saleRepository.countSalesByShiftDate(date);
        long units = saleRepository.sumQuantityByShiftDate(date);
        BigDecimal cash = saleRepository.sumCashAmountByShiftDate(date);
        return new ProductDayTotals(
                total != null ? total : BigDecimal.ZERO,
                saleCount,
                units,
                cash != null ? cash : BigDecimal.ZERO);
    }

    private Map<BillingPaymentType, BigDecimal> loadCashByBillingType(Long registerId) {
        Map<BillingPaymentType, BigDecimal> map = new java.util.EnumMap<>(BillingPaymentType.class);
        for (Object[] row : billingPaymentRepository.sumCashByPaymentTypeByCashRegisterId(registerId)) {
            if (row[0] instanceof BillingPaymentType type && row[1] != null) {
                map.put(type, (BigDecimal) row[1]);
            }
        }
        return map;
    }

    private BigDecimal sumFiadoCashCollected(LocalDate date) {
        BigDecimal sum =
                productCreditPaymentRepository.sumAmountByShiftDateAndMethod(date, PaymentMethod.CASH);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private WorkShift resolveReferenceShift(LocalDate date) {
        return workShiftRepository
                .findFirstByShiftDateAndStatusOrderByOpenedAtDesc(date, ShiftStatus.OPEN)
                .or(() -> workShiftRepository.findFirstByShiftDateAndStatusOrderByClosedAtDesc(
                        date, ShiftStatus.CLOSED))
                .orElseThrow(() -> new BusinessException(
                        "No hay turno del día para registrar el cierre. Abra y cierre al menos un turno en Ventas."));
    }

    private record ProductDayTotals(BigDecimal total, long saleCount, long units, BigDecimal cashTotal) {}

    private record FiadoDayTotals(
            BigDecimal total, Map<PaymentMethod, BigDecimal> byMethod, long paymentCount) {}
}
