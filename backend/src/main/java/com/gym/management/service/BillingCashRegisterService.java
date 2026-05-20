package com.gym.management.service;

import com.gym.management.dto.BillingCashRegisterExpenseRequest;
import com.gym.management.dto.BillingCashRegisterExpenseResponse;
import com.gym.management.dto.BillingCashRegisterResponse;
import com.gym.management.dto.OpenBillingCashRegisterRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.BillingCashRegisterExpenseMapper;
import com.gym.management.mapper.BillingCashRegisterMapper;
import com.gym.management.model.BillingCashRegister;
import com.gym.management.model.PaymentMethod;
import com.gym.management.model.BillingCashRegisterExpense;
import com.gym.management.model.Employee;
import com.gym.management.model.ShiftStatus;
import com.gym.management.repository.BillingCashRegisterExpenseRepository;
import com.gym.management.repository.BillingCashRegisterRepository;
import com.gym.management.repository.BillingPaymentRepository;
import com.gym.management.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingCashRegisterService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private final BillingCashRegisterRepository cashRegisterRepository;
    private final BillingPaymentRepository billingPaymentRepository;
    private final BillingCashRegisterExpenseRepository expenseRepository;
    private final EmployeeService employeeService;

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
                0L);
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

    @Transactional
    public BillingCashRegisterResponse close(Long id) {
        BillingCashRegister register = getRegister(id);
        if (!register.getRegisterDate().equals(today())) {
            throw new BusinessException("Solo se puede cerrar la caja del día actual");
        }
        if (register.getStatus() == ShiftStatus.CLOSED) {
            throw new BusinessException("La caja ya está cerrada");
        }
        register.setStatus(ShiftStatus.CLOSED);
        register.setClosedAt(LocalDateTime.now(GYM_ZONE));
        return toResponseWithTotals(cashRegisterRepository.save(register));
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

    /**
     * Facturación del día (membresías e ingresos): efectivo en caja = inicio + cobros efectivo − gastos efectivo.
     */
    @Transactional(readOnly = true)
    public BigDecimal cashInDrawerForToday() {
        return cashRegisterRepository
                .findFirstByRegisterDateOrderByOpenedAtDesc(today())
                .map(this::computeCashInDrawer)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal computeCashInDrawer(BillingCashRegister register) {
        Long id = register.getId();
        Map<PaymentMethod, BigDecimal> incomeByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        billingPaymentRepository.sumByPaymentMethodByCashRegisterId(id));
        Map<PaymentMethod, BigDecimal> expensesByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        expenseRepository.sumByPaymentMethodByCashRegisterId(id));
        BigDecimal cashIn = incomeByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        BigDecimal cashOut = expensesByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        return register.getOpeningCashAmount().add(cashIn).subtract(cashOut);
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
        return BillingCashRegisterMapper.toResponse(
                register, total, incomeByMethod, count, expenses, expensesByMethod, expenseCount);
    }
}
