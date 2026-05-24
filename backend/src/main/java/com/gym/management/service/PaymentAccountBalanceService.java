package com.gym.management.service;

import com.gym.management.dto.DigitalAccountBalanceLine;
import com.gym.management.mapper.SaleMapper;
import com.gym.management.model.BillingCashRegister;
import com.gym.management.model.PaymentAccountSettings;
import com.gym.management.model.PaymentMethod;
import com.gym.management.repository.BillingCashRegisterExpenseRepository;
import com.gym.management.repository.BillingCashRegisterOtherIncomeRepository;
import com.gym.management.repository.BillingCashRegisterRepository;
import com.gym.management.repository.BillingPaymentRepository;
import com.gym.management.repository.PaymentAccountSettingsRepository;
import com.gym.management.repository.ProductCreditPaymentRepository;
import com.gym.management.repository.SaleRepository;
import com.gym.management.util.MoneyUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentAccountBalanceService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");
    private static final LocalDate HISTORY_START = LocalDate.of(2020, 1, 1);
    private static final List<PaymentMethod> DIGITAL_METHODS = List.of(PaymentMethod.NEQUI, PaymentMethod.BANCOLOMBIA);

    private final PaymentAccountSettingsRepository settingsRepository;
    private final BillingCashRegisterRepository cashRegisterRepository;
    private final BillingPaymentRepository billingPaymentRepository;
    private final SaleRepository saleRepository;
    private final ProductCreditPaymentRepository productCreditPaymentRepository;
    private final BillingCashRegisterExpenseRepository expenseRepository;
    private final BillingCashRegisterOtherIncomeRepository otherIncomeRepository;

    @Transactional
    public PaymentAccountSettings getSettings() {
        return settingsRepository
                .findById(PaymentAccountSettings.SINGLETON_ID)
                .orElseGet(() -> settingsRepository.save(PaymentAccountSettings.builder()
                        .id(PaymentAccountSettings.SINGLETON_ID)
                        .nequiInitialBalance(BigDecimal.ZERO)
                        .bancolombiaInitialBalance(BigDecimal.ZERO)
                        .build()));
    }

    @Transactional
    public PaymentAccountSettings saveSettings(BigDecimal nequiInitial, BigDecimal bancolombiaInitial) {
        PaymentAccountSettings settings = settingsRepository
                .findById(PaymentAccountSettings.SINGLETON_ID)
                .orElse(PaymentAccountSettings.builder().id(PaymentAccountSettings.SINGLETON_ID).build());
        settings.setNequiInitialBalance(MoneyUtil.roundPesos(nequiInitial));
        settings.setBancolombiaInitialBalance(MoneyUtil.roundPesos(bancolombiaInitial));
        return settingsRepository.save(settings);
    }

    /** Saldos del día: apertura de caja + ingresos − gastos por medio digital. */
    @Transactional(readOnly = true)
    public List<DigitalAccountBalanceLine> computeDailyBalances(
            LocalDate date, Map<PaymentMethod, BigDecimal> incomeByMethod, Map<PaymentMethod, BigDecimal> expensesByMethod) {
        Optional<BillingCashRegister> register =
                cashRegisterRepository.findFirstByRegisterDateOrderByOpenedAtDesc(date);
        PaymentAccountSettings settings = getSettings();
        List<DigitalAccountBalanceLine> lines = new ArrayList<>();
        for (PaymentMethod method : DIGITAL_METHODS) {
            BigDecimal dayOpening = dayOpeningBalance(register, settings, method);
            BigDecimal income = MoneyUtil.roundPesos(incomeByMethod.getOrDefault(method, BigDecimal.ZERO));
            BigDecimal expense = MoneyUtil.roundPesos(expensesByMethod.getOrDefault(method, BigDecimal.ZERO));
            BigDecimal closing = MoneyUtil.roundPesos(dayOpening.add(income).subtract(expense));
            BigDecimal cumulative = computeCumulativeBalance(settings, method, date);
            lines.add(new DigitalAccountBalanceLine(
                    method,
                    SaleMapper.paymentMethodLabel(method),
                    dayOpening,
                    income,
                    expense,
                    closing,
                    cumulative));
        }
        return lines;
    }

    /** Saldos del mes: apertura del primer día con caja + movimientos del periodo. */
    @Transactional(readOnly = true)
    public List<DigitalAccountBalanceLine> computePeriodBalances(
            LocalDate start,
            LocalDate end,
            Map<PaymentMethod, BigDecimal> incomeByMethod,
            Map<PaymentMethod, BigDecimal> expensesByMethod) {
        Optional<BillingCashRegister> firstRegister =
                cashRegisterRepository.findFirstByRegisterDateBetweenOrderByRegisterDateAsc(start, end);
        PaymentAccountSettings settings = getSettings();
        List<DigitalAccountBalanceLine> lines = new ArrayList<>();
        for (PaymentMethod method : DIGITAL_METHODS) {
            BigDecimal periodOpening = periodOpeningBalance(firstRegister, settings, method, start);
            BigDecimal income = MoneyUtil.roundPesos(incomeByMethod.getOrDefault(method, BigDecimal.ZERO));
            BigDecimal expense = MoneyUtil.roundPesos(expensesByMethod.getOrDefault(method, BigDecimal.ZERO));
            BigDecimal closing = MoneyUtil.roundPesos(periodOpening.add(income).subtract(expense));
            BigDecimal cumulative = computeCumulativeBalance(settings, method, end);
            lines.add(new DigitalAccountBalanceLine(
                    method,
                    SaleMapper.paymentMethodLabel(method),
                    periodOpening,
                    income,
                    expense,
                    closing,
                    cumulative));
        }
        return lines;
    }

    public List<DigitalAccountBalanceLine> computeForOpenRegister(BillingCashRegister register) {
        LocalDate date = register.getRegisterDate();
        Map<PaymentMethod, BigDecimal> income = loadDayIncomeByMethod(date);
        Map<PaymentMethod, BigDecimal> expenses = BillingPaymentMethodTotals.fromAmountRows(
                expenseRepository.sumByPaymentMethodBetweenDates(date, date));
        return computeDailyBalances(date, income, expenses);
    }

    /**
     * Saldo del mes en curso por cuenta: saldo inicial global + ingresos del mes − gastos del mes.
     */
    @Transactional(readOnly = true)
    public List<DigitalAccountBalanceLine> computeCurrentMonthBalances() {
        LocalDate today = LocalDate.now(GYM_ZONE);
        LocalDate monthStart = today.withDayOfMonth(1);
        PaymentAccountSettings settings = getSettings();
        Map<PaymentMethod, BigDecimal> incomeByMethod = new EnumMap<>(PaymentMethod.class);
        Map<PaymentMethod, BigDecimal> expenseByMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : DIGITAL_METHODS) {
            incomeByMethod.put(method, sumIncomeBetween(monthStart, today, method));
            expenseByMethod.put(method, sumExpensesBetween(monthStart, today, method));
        }
        List<DigitalAccountBalanceLine> lines = new ArrayList<>();
        for (PaymentMethod method : DIGITAL_METHODS) {
            BigDecimal opening = globalInitial(settings, method);
            BigDecimal income = incomeByMethod.getOrDefault(method, BigDecimal.ZERO);
            BigDecimal expense = expenseByMethod.getOrDefault(method, BigDecimal.ZERO);
            BigDecimal closing = MoneyUtil.roundPesos(opening.add(income).subtract(expense));
            BigDecimal cumulative = computeCumulativeBalance(settings, method, today);
            lines.add(new DigitalAccountBalanceLine(
                    method,
                    SaleMapper.paymentMethodLabel(method),
                    opening,
                    income,
                    expense,
                    closing,
                    cumulative));
        }
        return lines;
    }

    private BigDecimal dayOpeningBalance(
            Optional<BillingCashRegister> register, PaymentAccountSettings settings, PaymentMethod method) {
        if (register.isPresent()) {
            BigDecimal fromRegister = openingFromRegister(register.get(), method);
            if (fromRegister.compareTo(BigDecimal.ZERO) > 0) {
                return fromRegister;
            }
        }
        return globalInitial(settings, method);
    }

    private BigDecimal periodOpeningBalance(
            Optional<BillingCashRegister> firstRegister,
            PaymentAccountSettings settings,
            PaymentMethod method,
            LocalDate periodStart) {
        if (firstRegister.isPresent()) {
            BigDecimal fromRegister = openingFromRegister(firstRegister.get(), method);
            if (fromRegister.compareTo(BigDecimal.ZERO) > 0) {
                return fromRegister;
            }
        }
        return openingBeforeDate(settings, method, periodStart);
    }

    /** Saldo acumulado al cierre del día: inicial global + ingresos históricos − gastos históricos. */
    private BigDecimal computeCumulativeBalance(PaymentAccountSettings settings, PaymentMethod method, LocalDate upTo) {
        BigDecimal initial = globalInitial(settings, method);
        BigDecimal income = sumIncomeBetween(HISTORY_START, upTo, method);
        BigDecimal expense = sumExpensesBetween(HISTORY_START, upTo, method);
        return MoneyUtil.roundPesos(initial.add(income).subtract(expense));
    }

    private BigDecimal openingBeforeDate(PaymentAccountSettings settings, PaymentMethod method, LocalDate before) {
        LocalDate end = before.minusDays(1);
        if (end.isBefore(HISTORY_START)) {
            return globalInitial(settings, method);
        }
        return computeCumulativeBalance(settings, method, end);
    }

    private BigDecimal openingFromRegister(BillingCashRegister register, PaymentMethod method) {
        return switch (method) {
            case NEQUI -> nullToZero(register.getOpeningNequiAmount());
            case BANCOLOMBIA -> nullToZero(register.getOpeningBancolombiaAmount());
            default -> BigDecimal.ZERO;
        };
    }

    private static BigDecimal globalInitial(PaymentAccountSettings settings, PaymentMethod method) {
        return switch (method) {
            case NEQUI -> nullToZero(settings.getNequiInitialBalance());
            case BANCOLOMBIA -> nullToZero(settings.getBancolombiaInitialBalance());
            default -> BigDecimal.ZERO;
        };
    }

    private Map<PaymentMethod, BigDecimal> loadDayIncomeByMethod(LocalDate date) {
        Map<PaymentMethod, BigDecimal> billing = BillingPaymentMethodTotals.fromAmountRows(
                billingPaymentRepository.sumByPaymentMethodBetweenDates(date, date));
        Map<PaymentMethod, BigDecimal> sales = BillingPaymentMethodTotals.fromAmountRows(
                saleRepository.sumByShiftDateGroupByPaymentMethod(date));
        Map<PaymentMethod, BigDecimal> fiado = BillingPaymentMethodTotals.fromAmountRows(
                productCreditPaymentRepository.sumByShiftDateGroupByPaymentMethod(date));
        Map<PaymentMethod, BigDecimal> other = BillingPaymentMethodTotals.fromAmountRows(
                otherIncomeRepository.sumByPaymentMethodBetweenDates(date, date));
        return BillingPaymentMethodTotals.mergeAll(billing, sales, fiado, other);
    }

    private BigDecimal sumIncomeBetween(LocalDate start, LocalDate end, PaymentMethod method) {
        Map<PaymentMethod, BigDecimal> billing = BillingPaymentMethodTotals.fromAmountRows(
                billingPaymentRepository.sumByPaymentMethodBetweenDates(start, end));
        Map<PaymentMethod, BigDecimal> sales = start.equals(end)
                ? BillingPaymentMethodTotals.fromAmountRows(saleRepository.sumByShiftDateGroupByPaymentMethod(start))
                : BillingPaymentMethodTotals.fromAmountRows(
                        saleRepository.sumByShiftDateBetweenGroupByPaymentMethod(start, end));
        Map<PaymentMethod, BigDecimal> fiado = start.equals(end)
                ? BillingPaymentMethodTotals.fromAmountRows(
                        productCreditPaymentRepository.sumByShiftDateGroupByPaymentMethod(start))
                : BillingPaymentMethodTotals.fromAmountRows(
                        productCreditPaymentRepository.sumByShiftDateBetweenGroupByPaymentMethod(start, end));
        Map<PaymentMethod, BigDecimal> other = BillingPaymentMethodTotals.fromAmountRows(
                otherIncomeRepository.sumByPaymentMethodBetweenDates(start, end));
        Map<PaymentMethod, BigDecimal> merged = BillingPaymentMethodTotals.mergeAll(billing, sales, fiado, other);
        return MoneyUtil.roundPesos(merged.getOrDefault(method, BigDecimal.ZERO));
    }

    private BigDecimal sumExpensesBetween(LocalDate start, LocalDate end, PaymentMethod method) {
        Map<PaymentMethod, BigDecimal> expenses = BillingPaymentMethodTotals.fromAmountRows(
                expenseRepository.sumByPaymentMethodBetweenDates(start, end));
        return MoneyUtil.roundPesos(expenses.getOrDefault(method, BigDecimal.ZERO));
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
