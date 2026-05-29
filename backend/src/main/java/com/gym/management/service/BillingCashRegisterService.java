package com.gym.management.service;

import com.gym.management.dto.BillingCashRegisterClosePreviewResponse;
import com.gym.management.dto.BillingHandoverCashBreakdown;
import com.gym.management.dto.BillingCashRegisterCloseResultResponse;
import com.gym.management.dto.ShiftOpenCashPreviewResponse;
import com.gym.management.dto.BillingCashRegisterExpenseRequest;
import com.gym.management.dto.BillingCashRegisterExpenseResponse;
import com.gym.management.dto.BillingCashRegisterOtherIncomeRequest;
import com.gym.management.dto.BillingCashRegisterOtherIncomeResponse;
import com.gym.management.dto.BillingCashRegisterResponse;
import com.gym.management.dto.CashShortfallResponse;
import com.gym.management.dto.CloseBillingCashRegisterRequest;
import com.gym.management.dto.OpenBillingCashRegisterRequest;
import com.gym.management.model.ShiftHandover;
import com.gym.management.model.WorkShift;
import com.gym.management.repository.ProductCreditPaymentRepository;
import com.gym.management.repository.ShiftHandoverRepository;
import com.gym.management.util.CashCountUtil;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.BillingCashRegisterExpenseMapper;
import com.gym.management.mapper.BillingCashRegisterMapper;
import com.gym.management.mapper.BillingCashRegisterOtherIncomeMapper;
import com.gym.management.model.BillingCashRegister;
import com.gym.management.model.BillingPaymentType;
import com.gym.management.model.CashShortfallKind;
import com.gym.management.model.PaymentMethod;
import com.gym.management.model.BillingCashRegisterExpense;
import com.gym.management.model.BillingCashRegisterOtherIncome;
import com.gym.management.model.Employee;
import com.gym.management.model.ShiftStatus;
import com.gym.management.repository.BillingCashRegisterExpenseRepository;
import com.gym.management.repository.BillingCashRegisterOtherIncomeRepository;
import com.gym.management.repository.BillingCashRegisterRepository;
import com.gym.management.repository.EmployeeCashShortfallRepository;
import com.gym.management.repository.BillingPaymentRepository;
import com.gym.management.repository.SaleRepository;
import com.gym.management.repository.WorkShiftRepository;
import com.gym.management.security.SecurityUtils;
import com.gym.management.util.MoneyUtil;
import com.gym.management.util.TreasuryAccess;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingCashRegisterService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");
    private static final String AUTO_SURPLUS_PREFIX = "[AUTO:SOBRANTE";
    private static final String SURPLUS_HANDOVER_TAG_PREFIX = "[AUTO:SOBRANTE_ENTREGA:";
    private static final String SURPLUS_SHIFT_OPEN_TAG_PREFIX = "[AUTO:SOBRANTE_APERTURA:";
    private static final String SURPLUS_CASH_CLOSE_TAG_PREFIX = "[AUTO:SOBRANTE_CIERRE_CAJA:";
    private static final String SHORTFALL_HANDOVER_EXPENSE_TAG_PREFIX = "[AUTO:FALTANTE_ENTREGA:";
    private static final Set<CashShortfallKind> CASH_DRAWER_SHORTFALL_KINDS = EnumSet.of(
            CashShortfallKind.CASH_REGISTER, CashShortfallKind.CASH_SHIFT_OPEN);

    private final BillingCashRegisterRepository cashRegisterRepository;
    private final BillingPaymentRepository billingPaymentRepository;
    private final SaleRepository saleRepository;
    private final WorkShiftRepository workShiftRepository;
    private final BillingCashRegisterExpenseRepository expenseRepository;
    private final BillingCashRegisterOtherIncomeRepository otherIncomeRepository;
    private final ProductCreditPaymentRepository productCreditPaymentRepository;
    private final EmployeeService employeeService;
    private final CashShortfallService cashShortfallService;
    private final EmployeeCashShortfallRepository shortfallRepository;
    private final ShiftHandoverRepository handoverRepository;
    private final PaymentAccountBalanceService paymentAccountBalanceService;

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
        BillingHandoverCashBreakdown billingCash = computeHandoverCashBreakdown(register);
        BigDecimal closedShiftsNet = sumClosedShiftsCashNet(date);

        BigDecimal lastHandoverCash = BigDecimal.ZERO;
        BigDecimal cashSinceHandover = BigDecimal.ZERO;
        BigDecimal systemCash;
        BigDecimal deducted;
        BigDecimal expected;

        Instant registerOpened = register.getOpenedAt().atZone(GYM_ZONE).toInstant();
        Instant handoverAnchor = null;
        if (register.getLastHandoverCashAmount() != null && register.getLastHandoverAt() != null) {
            lastHandoverCash = MoneyUtil.roundPesos(register.getLastHandoverCashAmount());
            handoverAnchor = register.getLastHandoverAt();
        } else {
            Optional<ShiftHandover> lastHandover = resolveLastHandoverForOpen(register, date, registerOpened);
            if (lastHandover.isPresent()) {
                lastHandoverCash = MoneyUtil.roundPesos(CashCountUtil.totalCash(lastHandover.get()));
                handoverAnchor = lastHandover.get().getSubmittedAt();
            }
        }
        if (handoverAnchor != null) {
            cashSinceHandover = computeCashSinceHandover(id, date, handoverAnchor);
            systemCash = MoneyUtil.roundPesos(lastHandoverCash.add(cashSinceHandover));
            deducted = sumCashShortfallsAfter(date, handoverAnchor);
            expected = netCashExpectedAfterShortfalls(systemCash, deducted);
        } else {
            systemCash = MoneyUtil.roundPesos(
                    billingCash.total().add(closedShiftsNet).add(fiadoCash));
            deducted = sumRegisteredCashShortfallsForDate(date);
            expected = netCashExpectedAfterShortfalls(systemCash, deducted);
        }

        Employee opener = register.getOpenedBy();
        String openerName = opener.getFirstName() + " " + opener.getLastName();
        return new ShiftOpenCashPreviewResponse(
                id,
                openerName,
                register.getOpeningCashAmount(),
                cashExpenses,
                productTotals.cashTotal(),
                lastHandoverCash,
                cashSinceHandover,
                closedShiftsNet,
                fiadoCash,
                cashByType.getOrDefault(BillingPaymentType.MEMBERSHIP, BigDecimal.ZERO),
                cashByType.getOrDefault(BillingPaymentType.DAY_WORKOUT, BigDecimal.ZERO),
                cashByType.getOrDefault(BillingPaymentType.SPORTS_DANCE, BigDecimal.ZERO),
                billingCash.otherIncomesCash(),
                systemCash,
                deducted,
                expected);
    }

    /**
     * Guarda en la caja del día el efectivo físico contado en una entrega de turno (base al abrir el siguiente turno).
     */
    @Transactional
    public void recordLastHandoverPhysicalCash(LocalDate registerDate, BigDecimal physicalCash, Instant submittedAt) {
        if (physicalCash == null || physicalCash.compareTo(BigDecimal.ZERO) < 0 || submittedAt == null) {
            return;
        }
        LocalDate date = registerDate != null ? registerDate : today();
        cashRegisterRepository
                .findFirstByRegisterDateOrderByOpenedAtDesc(date)
                .ifPresent(register -> {
                    register.setLastHandoverCashAmount(MoneyUtil.roundPesos(physicalCash));
                    register.setLastHandoverAt(submittedAt);
                    cashRegisterRepository.save(register);
                });
    }

    private Optional<ShiftHandover> resolveLastHandoverForOpen(
            BillingCashRegister register, LocalDate shiftDate, Instant registerOpened) {
        Optional<ShiftHandover> sameDay =
                handoverRepository.findFirstByWorkShift_ShiftDateOrderBySubmittedAtDesc(shiftDate);
        if (sameDay.isPresent()) {
            return sameDay;
        }
        Optional<ShiftHandover> beforeRegisterOpen =
                handoverRepository.findFirstBySubmittedAtLessThanOrderBySubmittedAtDesc(registerOpened);
        if (beforeRegisterOpen.isPresent()) {
            return beforeRegisterOpen;
        }
        return handoverRepository.findFirstByWorkShift_ShiftDateOrderBySubmittedAtDesc(shiftDate);
    }

    private BigDecimal computeCashSinceHandover(Long registerId, LocalDate date, Instant after) {
        BigDecimal billingIn = nullToZero(billingPaymentRepository.sumCashAmountByCashRegisterIdAfter(registerId, after));
        BigDecimal productIn = nullToZero(saleRepository.sumCashAmountByShiftDateAfter(date, after));
        BigDecimal otherIn = nullToZero(
                otherIncomeRepository.sumCashAmountByCashRegisterIdAfterExcludingAutoSurplus(registerId, after));
        BigDecimal fiadoIn = nullToZero(productCreditPaymentRepository.sumCashAmountByShiftDateAfter(date, after));
        BigDecimal cashOut = nullToZero(expenseRepository.sumCashAmountByCashRegisterIdAfter(registerId, after));
        return MoneyUtil.roundPesos(billingIn.add(productIn).add(otherIn).add(fiadoIn).subtract(cashOut));
    }

    private BigDecimal sumCashShortfallsAfter(LocalDate date, Instant after) {
        BigDecimal sum = shortfallRepository.sumShortfallAmountByRecordDateAndKindsAfter(
                date, CASH_DRAWER_SHORTFALL_KINDS, after);
        return sum != null ? MoneyUtil.roundPesos(sum) : BigDecimal.ZERO;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /** Efectivo de ventas en turnos ya cerrados hoy (neto, sin faltantes de efectivo ya registrados). */
    private BigDecimal sumClosedShiftsCashNet(LocalDate date) {
        List<WorkShift> closed =
                workShiftRepository.findByShiftDateAndStatusOrderByOpenedAtAsc(date, ShiftStatus.CLOSED);
        BigDecimal netTotal = BigDecimal.ZERO;
        for (WorkShift shift : closed) {
            BigDecimal cash = saleRepository.sumTotalByPaymentMethodAndShift(PaymentMethod.CASH, shift.getId());
            if (cash == null) {
                cash = BigDecimal.ZERO;
            }
            BigDecimal shortfall = shortfallRepository.sumShortfallAmountByWorkShiftIdAndKinds(
                    shift.getId(), CASH_DRAWER_SHORTFALL_KINDS);
            BigDecimal net = MoneyUtil.roundPesos(cash.subtract(shortfall));
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                netTotal = netTotal.add(net);
            }
        }
        return MoneyUtil.roundPesos(netTotal);
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
                .openingNequiAmount(BigDecimal.ZERO)
                .openingBancolombiaAmount(BigDecimal.ZERO)
                .openedAt(LocalDateTime.now(GYM_ZONE))
                .status(ShiftStatus.OPEN)
                .build();
        return TreasuryAccess.maskRegisterResponse(BillingCashRegisterMapper.toResponse(
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
                BigDecimal.ZERO,
                BillingPaymentMethodTotals.emptyBillableMap(),
                BillingPaymentMethodTotals.emptyBillableMap(),
                0L,
                BillingPaymentMethodTotals.emptyBillableMap(),
                BigDecimal.ZERO,
                request.openingCashAmount(),
                null,
                null,
                List.of()));
    }

    @Transactional(readOnly = true)
    public List<BillingCashRegisterOtherIncomeResponse> listOtherIncomesByDate(LocalDate date) {
        LocalDate target = date != null ? date : today();
        return otherIncomeRepository.findByRegisterDateWithDetails(target).stream()
                .map(BillingCashRegisterOtherIncomeMapper::toResponse)
                .toList();
    }

    @Transactional
    public BillingCashRegisterOtherIncomeResponse addOtherIncome(BillingCashRegisterOtherIncomeRequest request) {
        BillingCashRegister register = getOpenRegisterRequired();
        Employee recorder = resolveCurrentEmployee();
        PaymentMethod method = request.paymentMethod();
        BillingPaymentMethodRules.requireAllowed(method);
        BillingCashRegisterOtherIncome income = BillingCashRegisterOtherIncome.builder()
                .cashRegister(register)
                .amount(request.amount())
                .paymentMethod(method)
                .observation(request.observation().trim())
                .recordedBy(recorder)
                .build();
        return BillingCashRegisterOtherIncomeMapper.toResponse(otherIncomeRepository.save(income));
    }

    /**
     * Registra el sobrante en efectivo de una entrega de turno como otro ingreso en caja (efectivo), visible en
     * Facturación.
     */
    @Transactional
    public Optional<BillingCashRegisterOtherIncomeResponse> registerHandoverCashSurplus(
            ShiftHandover handover, BigDecimal expectedCash, BigDecimal declaredCash) {
        if (handover.getId() == null || declaredCash.compareTo(expectedCash) <= 0) {
            return Optional.empty();
        }
        BigDecimal surplus = MoneyUtil.roundPesos(declaredCash.subtract(expectedCash));
        return registerHandoverCashSurplusAmount(handover, surplus);
    }

    /**
     * Registra en Facturación el sobrante en efectivo que queda en caja (tras cruzar con faltante de inventario, si
     * aplica).
     */
    @Transactional
    public Optional<BillingCashRegisterOtherIncomeResponse> registerHandoverCashSurplusAmount(
            ShiftHandover handover, BigDecimal surplusAmount) {
        if (handover.getId() == null) {
            return Optional.empty();
        }
        BigDecimal surplus = MoneyUtil.roundPesos(surplusAmount);
        if (surplus.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        BillingCashRegister register = requireRegisterForDate(handover.getWorkShift().getShiftDate());
        String tag = surplusHandoverTag(handover.getId());
        String label =
                "Sobrante en efectivo — entrega de turno " + handover.getWorkShift().getName();
        return registerCashSurplusIfNew(register, handover.getEmployee(), surplus, tag, label);
    }

    /**
     * Registra el sobrante en efectivo al abrir turno (conteo vs. esperado) como otro ingreso en caja del día.
     */
    @Transactional
    public Optional<BillingCashRegisterOtherIncomeResponse> registerShiftOpenCashSurplus(
            BillingCashRegister register,
            WorkShift previousShift,
            BigDecimal expectedCash,
            BigDecimal declaredCash,
            Employee recordedBy) {
        if (declaredCash.compareTo(expectedCash) <= 0) {
            return Optional.empty();
        }
        BigDecimal surplus = MoneyUtil.roundPesos(declaredCash.subtract(expectedCash));
        if (surplus.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        String tag = surplusShiftOpenTag(previousShift.getId(), register.getRegisterDate());
        String label = "Sobrante en efectivo — apertura de turno (después de " + previousShift.getName() + ")";
        return registerCashSurplusIfNew(register, recordedBy, surplus, tag, label);
    }

    @Transactional(readOnly = true)
    public Optional<BillingCashRegisterOtherIncomeResponse> findHandoverCashSurplusIncome(Long handoverId) {
        if (handoverId == null) {
            return Optional.empty();
        }
        return otherIncomeRepository.findByObservationStartingWith(surplusHandoverTag(handoverId)).stream()
                .findFirst()
                .map(BillingCashRegisterOtherIncomeMapper::toResponse);
    }

    @Transactional
    public void deleteHandoverCashSurplusIncome(Long handoverId) {
        if (handoverId != null) {
            otherIncomeRepository.deleteByObservationStartingWith(surplusHandoverTag(handoverId));
        }
    }

    public static String surplusHandoverTag(Long handoverId) {
        return SURPLUS_HANDOVER_TAG_PREFIX + handoverId + "]";
    }

    public static String shortfallHandoverExpenseTag(Long handoverId) {
        return SHORTFALL_HANDOVER_EXPENSE_TAG_PREFIX + handoverId + "]";
    }

    @Transactional
    public void registerHandoverCashShortfallExpense(ShiftHandover handover, BigDecimal shortfallAmount) {
        if (handover == null || handover.getId() == null) {
            return;
        }
        BigDecimal amount = MoneyUtil.roundPesos(shortfallAmount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BillingCashRegister register = requireRegisterForDate(handover.getWorkShift().getShiftDate());
        String tag = shortfallHandoverExpenseTag(handover.getId());
        if (expenseRepository.existsByObservationStartingWith(tag)) {
            return;
        }
        String employeeName = handover.getEmployee().getFirstName() + " " + handover.getEmployee().getLastName();
        String label =
                "Faltante en efectivo — entrega de turno "
                        + handover.getWorkShift().getName()
                        + " (responsable: "
                        + employeeName
                        + ")";
        BillingCashRegisterExpense expense = BillingCashRegisterExpense.builder()
                .cashRegister(register)
                .amount(amount)
                .paymentMethod(PaymentMethod.CASH)
                .observation(tag + " " + label)
                .recordedBy(handover.getEmployee())
                .build();
        expenseRepository.save(expense);
    }

    @Transactional
    public void deleteHandoverCashShortfallExpense(Long handoverId) {
        if (handoverId != null) {
            expenseRepository.deleteByObservationStartingWith(shortfallHandoverExpenseTag(handoverId));
        }
    }

    private static String surplusShiftOpenTag(Long previousShiftId, LocalDate registerDate) {
        return SURPLUS_SHIFT_OPEN_TAG_PREFIX + previousShiftId + ":" + registerDate + "]";
    }

    private static String surplusCashCloseTag(Long registerId) {
        return SURPLUS_CASH_CLOSE_TAG_PREFIX + registerId + "]";
    }

    @Transactional
    public Optional<BillingCashRegisterOtherIncomeResponse> registerCashRegisterCloseSurplus(
            BillingCashRegister register, Employee recordedBy, BigDecimal expectedCash, BigDecimal declaredCash) {
        if (register == null || recordedBy == null || declaredCash.compareTo(expectedCash) <= 0) {
            return Optional.empty();
        }
        BigDecimal surplus = MoneyUtil.roundPesos(declaredCash.subtract(expectedCash));
        if (surplus.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        String tag = surplusCashCloseTag(register.getId());
        String label = "Sobrante en efectivo — cierre de caja";
        return registerCashSurplusIfNew(register, recordedBy, surplus, tag, label);
    }

    private Optional<BillingCashRegisterOtherIncomeResponse> registerCashSurplusIfNew(
            BillingCashRegister register,
            Employee recordedBy,
            BigDecimal surplus,
            String tag,
            String label) {
        if (otherIncomeRepository.existsByObservationStartingWith(tag)) {
            return otherIncomeRepository.findByObservationStartingWith(tag).stream()
                    .findFirst()
                    .map(BillingCashRegisterOtherIncomeMapper::toResponse);
        }
        BillingCashRegisterOtherIncome income = BillingCashRegisterOtherIncome.builder()
                .cashRegister(register)
                .amount(surplus)
                .paymentMethod(PaymentMethod.CASH)
                .observation(tag + " " + label)
                .recordedBy(recordedBy)
                .build();
        return Optional.of(
                BillingCashRegisterOtherIncomeMapper.toResponse(otherIncomeRepository.save(income)));
    }

    private BillingCashRegister requireRegisterForDate(LocalDate date) {
        return cashRegisterRepository
                .findFirstByRegisterDateOrderByOpenedAtDesc(date)
                .orElseThrow(() -> new BusinessException(
                        "No hay caja de facturación para el día " + date + ". Abra la caja del día en Facturación."));
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
        BigDecimal cashInDrawer = MoneyUtil.roundPesos(computeCashInDrawer(register));
        BigDecimal fiadoCash = MoneyUtil.roundPesos(sumFiadoCashCollected(register.getRegisterDate()));
        List<com.gym.management.dto.DigitalAccountBalanceLine> digitalAccounts =
                paymentAccountBalanceService.computeForOpenRegister(register);
        return TreasuryAccess.maskClosePreview(new BillingCashRegisterClosePreviewResponse(
                cashInDrawer,
                fiadoCash,
                cashInDrawer,
                shiftInventoryService.listActiveProductLines(),
                digitalAccounts));
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

        BigDecimal expectedCash = MoneyUtil.roundPesos(computeCashInDrawer(register));
        BigDecimal declaredCash = MoneyUtil.roundPesos(request.cashCount().totalCash());

        ShiftInventoryService.InventoryCloseAtRegisterResult inventoryResult =
                shiftInventoryService.processAtCashRegisterClose(
                        register.getRegisterDate(), request.inventoryCounts(), closer, referenceShift);

        Optional<CashShortfallResponse> cashShortfall = cashShortfallService.registerFromCashRegisterClose(
                register, closer, referenceShift, expectedCash, declaredCash);
        registerCashRegisterCloseSurplus(register, closer, expectedCash, declaredCash);

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
                .map(reg -> resolveCashInDrawerTotals(reg, false).total())
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Base de caja para entrega de turno: apertura + cobros de facturación en efectivo + otros ingresos en
     * efectivo − gastos en efectivo. Sin ventas de productos ni fiado (esos se suman por turno en
     * {@link ShiftHandoverService}).
     */
    @Transactional(readOnly = true)
    public BigDecimal billingCashExpectedForHandover() {
        return billingHandoverCashBreakdown().total();
    }

    @Transactional(readOnly = true)
    public BillingHandoverCashBreakdown billingHandoverCashBreakdown() {
        return cashRegisterRepository
                .findFirstByRegisterDateOrderByOpenedAtDesc(today())
                .map(this::computeHandoverCashBreakdown)
                .orElse(new BillingHandoverCashBreakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private BigDecimal computeBillingCashForHandover(BillingCashRegister register) {
        return computeHandoverCashBreakdown(register).total();
    }

    private BillingHandoverCashBreakdown computeHandoverCashBreakdown(BillingCashRegister register) {
        Long id = register.getId();
        Map<PaymentMethod, BigDecimal> incomeByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        billingPaymentRepository.sumByPaymentMethodByCashRegisterId(id));
        Map<PaymentMethod, BigDecimal> expensesByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        expenseRepository.sumByPaymentMethodByCashRegisterId(id));
        Map<PaymentMethod, BigDecimal> otherIncomesByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        otherIncomeRepository.sumByPaymentMethodByCashRegisterId(id));
        BigDecimal billingCashIn = incomeByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        BigDecimal otherCashIn = otherIncomesByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        BigDecimal cashOut = expensesByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        BigDecimal cashBase = MoneyUtil.roundPesos(
                register.getOpeningCashAmount().add(billingCashIn).subtract(cashOut));
        BigDecimal otherIncomesCash = MoneyUtil.roundPesos(otherCashIn);
        BigDecimal total = MoneyUtil.roundPesos(cashBase.add(otherIncomesCash));
        return new BillingHandoverCashBreakdown(cashBase, otherIncomesCash, total);
    }

    private BigDecimal computeCashInDrawer(BillingCashRegister register) {
        return resolveCashInDrawerTotals(register, true).total();
    }

    private BigDecimal computeCashInDrawerFormula(BillingCashRegister register, boolean includeFiadoCash) {
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
        Map<PaymentMethod, BigDecimal> otherIncomesByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        otherIncomeRepository.sumByPaymentMethodByCashRegisterId(id));
        BigDecimal otherCashIn = otherIncomesByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        BigDecimal total = register.getOpeningCashAmount()
                .add(billingCashIn)
                .add(productCash)
                .add(otherCashIn)
                .subtract(cashOut);
        if (includeFiadoCash) {
            total = total.add(sumFiadoCashCollected(register.getRegisterDate()));
        }
        return MoneyUtil.roundPesos(total);
    }

    private BillingCashRegisterResponse toResponseWithTotals(BillingCashRegister register) {
        return maskTreasury(buildRegisterResponseWithTotals(register));
    }

    private BillingCashRegisterResponse maskTreasury(BillingCashRegisterResponse response) {
        return TreasuryAccess.maskRegisterResponse(response);
    }

    private BillingCashRegisterResponse buildRegisterResponseWithTotals(BillingCashRegister register) {
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
        Map<PaymentMethod, BigDecimal> productSalesByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        saleRepository.sumByShiftDateGroupByPaymentMethod(register.getRegisterDate()));
        Map<PaymentMethod, BigDecimal> otherIncomesByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        otherIncomeRepository.sumByPaymentMethodByCashRegisterId(id));
        Map<PaymentMethod, BigDecimal> autoSurplusByMethod = sumAutoSurplusByMethod(id, register.getRegisterDate());
        BigDecimal otherIncomesTotal = BillingPaymentMethodTotals.sum(otherIncomesByMethod);
        long otherIncomeCount = otherIncomeRepository.countByCashRegisterId(id);
        Map<PaymentMethod, BigDecimal> dayIncomeByMethod = BillingPaymentMethodTotals.mergeAll(
                incomeByMethod, fiadoTotals.byMethod(), productSalesByMethod, otherIncomesByMethod);
        BigDecimal dayIncomeTotal = BillingPaymentMethodTotals.sum(dayIncomeByMethod);
        CashInDrawerTotals cashTotals = resolveCashInDrawerTotals(register, true);
        List<com.gym.management.dto.DigitalAccountBalanceLine> digitalAccounts =
                paymentAccountBalanceService.computeForOpenRegister(register);
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
                otherIncomesTotal,
                otherIncomesByMethod,
                autoSurplusByMethod,
                otherIncomeCount,
                dayIncomeByMethod,
                dayIncomeTotal,
                cashTotals.total(),
                cashTotals.lastHandoverCash(),
                cashTotals.cashSinceHandover(),
                digitalAccounts);
    }

    private Map<PaymentMethod, BigDecimal> sumAutoSurplusByMethod(Long registerId, LocalDate registerDate) {
        Map<PaymentMethod, BigDecimal> totals = new java.util.EnumMap<>(PaymentMethod.class);
        otherIncomeRepository.findByRegisterDateWithDetails(registerDate).stream()
                .filter(row -> row.getCashRegister() != null && registerId.equals(row.getCashRegister().getId()))
                .filter(row -> row.getObservation() != null && row.getObservation().startsWith(AUTO_SURPLUS_PREFIX))
                .forEach(row -> totals.merge(row.getPaymentMethod(), row.getAmount(), BigDecimal::add));
        return totals;
    }

    @Transactional(readOnly = true)
    public com.gym.management.dto.CashInDrawerTotals cashInDrawerTotalsForDate(LocalDate date) {
        LocalDate target = date != null ? date : today();
        return cashRegisterRepository
                .findFirstByRegisterDateOrderByOpenedAtDesc(target)
                .map(reg -> toCashInDrawerTotals(resolveCashInDrawerTotals(reg, true)))
                .orElse(new com.gym.management.dto.CashInDrawerTotals(
                        BigDecimal.ZERO, null, null));
    }

    @Transactional(readOnly = true)
    public BillingHandoverCashBreakdown billingHandoverCashBreakdownForDate(LocalDate date) {
        LocalDate target = date != null ? date : today();
        return cashRegisterRepository
                .findFirstByRegisterDateOrderByOpenedAtDesc(target)
                .map(this::computeHandoverCashBreakdown)
                .orElse(new BillingHandoverCashBreakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private com.gym.management.dto.CashInDrawerTotals toCashInDrawerTotals(CashInDrawerTotals internal) {
        return new com.gym.management.dto.CashInDrawerTotals(
                internal.total(), internal.lastHandoverCash(), internal.cashSinceHandover());
    }

    private record CashInDrawerTotals(
            BigDecimal total, BigDecimal lastHandoverCash, BigDecimal cashSinceHandover) {}

    private CashInDrawerTotals resolveCashInDrawerTotals(BillingCashRegister register, boolean includeFiadoCash) {
        // Regla de negocio vigente:
        // "Efectivo en caja" siempre acumula desde la apertura de caja del día.
        // La última entrega de turno se conserva solo como referencia informativa.
        BigDecimal total = computeCashInDrawerFormula(register, includeFiadoCash);
        BigDecimal lastHandoverCash = register.getLastHandoverCashAmount();
        Instant lastHandoverAt = register.getLastHandoverAt();
        if (lastHandoverCash == null || lastHandoverAt == null) {
            Optional<ShiftHandover> latest =
                    handoverRepository.findFirstByWorkShift_ShiftDateOrderBySubmittedAtDesc(register.getRegisterDate());
            if (latest.isPresent()) {
                lastHandoverCash = MoneyUtil.roundPesos(CashCountUtil.totalCash(latest.get()));
                lastHandoverAt = latest.get().getSubmittedAt();
            }
        }
        if (lastHandoverCash != null && lastHandoverAt != null) {
            BigDecimal since = computeCashSinceHandover(register.getId(), register.getRegisterDate(), lastHandoverAt);
            if (!includeFiadoCash) {
                BigDecimal fiadoSince =
                        nullToZero(productCreditPaymentRepository.sumCashAmountByShiftDateAfter(
                                register.getRegisterDate(), lastHandoverAt));
                since = MoneyUtil.roundPesos(since.subtract(fiadoSince));
            }
            return new CashInDrawerTotals(total, lastHandoverCash, since);
        }
        return new CashInDrawerTotals(total, null, null);
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

    private BigDecimal sumRegisteredCashShortfallsForDate(LocalDate date) {
        BigDecimal sum = shortfallRepository.sumShortfallAmountByRecordDateAndKinds(date, CASH_DRAWER_SHORTFALL_KINDS);
        return sum != null ? MoneyUtil.roundPesos(sum) : BigDecimal.ZERO;
    }

    private static BigDecimal netCashExpectedAfterShortfalls(BigDecimal systemCash, BigDecimal deducted) {
        BigDecimal net = MoneyUtil.roundPesos(systemCash.subtract(deducted));
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
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
