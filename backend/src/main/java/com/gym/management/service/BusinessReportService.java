package com.gym.management.service;

import com.gym.management.dto.BillingTypeReportSection;
import com.gym.management.dto.BusinessReportBreakdown;
import com.gym.management.dto.DailyBusinessReportResponse;
import com.gym.management.dto.DigitalAccountBalanceLine;
import com.gym.management.dto.MembershipPlanReportLine;
import com.gym.management.dto.MonthlyBusinessReportResponse;
import com.gym.management.dto.ProductInventoryReportLine;
import com.gym.management.dto.ProductSaleByPaymentLine;
import com.gym.management.dto.ProductSalesReportSection;
import com.gym.management.exception.BusinessException;
import com.gym.management.model.BillingPaymentType;
import com.gym.management.model.PaymentMethod;
import com.gym.management.model.Product;
import com.gym.management.repository.BillingCashRegisterExpenseRepository;
import com.gym.management.repository.BillingCashRegisterOtherIncomeRepository;
import com.gym.management.repository.BillingCashRegisterRepository;
import com.gym.management.repository.BillingPaymentRepository;
import com.gym.management.repository.ProductCreditPaymentRepository;
import com.gym.management.repository.ProductRepository;
import com.gym.management.repository.SaleRepository;
import com.gym.management.util.TreasuryAccess;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessReportService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private final BillingPaymentRepository billingPaymentRepository;
    private final SaleRepository saleRepository;
    private final ProductCreditPaymentRepository productCreditPaymentRepository;
    private final BillingCashRegisterExpenseRepository expenseRepository;
    private final BillingCashRegisterOtherIncomeRepository otherIncomeRepository;
    private final BillingCashRegisterRepository cashRegisterRepository;
    private final ProductRepository productRepository;
    private final PaymentAccountBalanceService paymentAccountBalanceService;

    @Transactional(readOnly = true)
    public DailyBusinessReportResponse dailyReport(LocalDate date) {
        requireTreasuryAccess();
        LocalDate target = date != null ? date : LocalDate.now(GYM_ZONE);
        PeriodTotals totals = loadPeriodTotals(target, target);
        CashRegisterSnapshot cashRegister = resolveCashRegister(target);
        List<ProductInventoryReportLine> inventory = buildInventoryForSingleDate(target);
        BusinessReportBreakdown breakdown = loadBreakdown(target, target);
        List<DigitalAccountBalanceLine> digitalAccounts = paymentAccountBalanceService.computeDailyBalances(
                target, totals.totalIncomeByMethod(), totals.expensesByMethod());

        return new DailyBusinessReportResponse(
                target,
                cashRegister.status(),
                cashRegister.openingCashAmount(),
                totals.billingPaymentCount(),
                totals.billingIncomeTotal(),
                totals.billingIncomeByMethod(),
                totals.productSales(),
                totals.fiadoTotal(),
                totals.fiadoByMethod(),
                totals.expenseCount(),
                totals.expensesTotal(),
                totals.expensesByMethod(),
                totals.totalIncome(),
                totals.totalIncomeByMethod(),
                totals.netResult(),
                inventory,
                breakdown,
                digitalAccounts);
    }

    @Transactional(readOnly = true)
    public MonthlyBusinessReportResponse monthlyReport(int year, int month) {
        requireTreasuryAccess();
        YearMonth period = YearMonth.of(year, month);
        LocalDate start = period.atDay(1);
        LocalDate end = period.atEndOfMonth();
        PeriodTotals totals = loadPeriodTotals(start, end);
        long cashRegisterDays = cashRegisterRepository.countByRegisterDateBetween(start, end);
        List<ProductInventoryReportLine> inventory = buildInventoryForDateRange(start, end);
        BusinessReportBreakdown breakdown = loadBreakdown(start, end);
        List<DigitalAccountBalanceLine> digitalAccounts = paymentAccountBalanceService.computePeriodBalances(
                start, end, totals.totalIncomeByMethod(), totals.expensesByMethod());

        return new MonthlyBusinessReportResponse(
                year,
                month,
                start,
                end,
                cashRegisterDays,
                totals.billingPaymentCount(),
                totals.billingIncomeTotal(),
                totals.billingIncomeByMethod(),
                totals.productSales(),
                totals.fiadoTotal(),
                totals.fiadoByMethod(),
                totals.expenseCount(),
                totals.expensesTotal(),
                totals.expensesByMethod(),
                totals.totalIncome(),
                totals.totalIncomeByMethod(),
                totals.netResult(),
                inventory,
                breakdown,
                digitalAccounts);
    }

    private static void requireTreasuryAccess() {
        if (!TreasuryAccess.canViewTreasuryBalances()) {
            throw new BusinessException("Solo administración puede ver reportes de tesorería");
        }
    }

    private BusinessReportBreakdown loadBreakdown(LocalDate start, LocalDate end) {
        BillingTypeReportSection dayWorkout =
                loadBillingTypeSection(start, end, BillingPaymentType.DAY_WORKOUT);
        BillingTypeReportSection sportsDance =
                loadBillingTypeSection(start, end, BillingPaymentType.SPORTS_DANCE);
        BillingTypeReportSection membership =
                loadBillingTypeSection(start, end, BillingPaymentType.MEMBERSHIP);
        BillingTypeReportSection otherIncomes = loadOtherIncomesSection(start, end);
        List<MembershipPlanReportLine> membershipByPlan = loadMembershipByPlan(start, end);
        List<ProductSaleByPaymentLine> productSalesByPayment = loadProductSalesByPayment(start, end);
        return new BusinessReportBreakdown(
                dayWorkout, sportsDance, membership, otherIncomes, membershipByPlan, productSalesByPayment);
    }

    private BillingTypeReportSection loadOtherIncomesSection(LocalDate start, LocalDate end) {
        long count = otherIncomeRepository.countBetweenDates(start, end);
        Map<PaymentMethod, BigDecimal> byMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        otherIncomeRepository.sumByPaymentMethodBetweenDates(start, end));
        BigDecimal total = BillingPaymentMethodTotals.sum(byMethod);
        return new BillingTypeReportSection(count, total, byMethod);
    }

    private BillingTypeReportSection loadBillingTypeSection(
            LocalDate start, LocalDate end, BillingPaymentType type) {
        long count = billingPaymentRepository.countByTypeBetweenDates(start, end, type);
        Map<PaymentMethod, BigDecimal> byMethod = BillingPaymentMethodTotals.fromAmountRows(
                billingPaymentRepository.sumByPaymentMethodBetweenDatesAndType(start, end, type));
        BigDecimal total = BillingPaymentMethodTotals.sum(byMethod);
        return new BillingTypeReportSection(count, total, byMethod);
    }

    private List<MembershipPlanReportLine> loadMembershipByPlan(LocalDate start, LocalDate end) {
        Map<String, PlanAggregate> aggregates = new LinkedHashMap<>();
        for (Object[] row :
                billingPaymentRepository.aggregateMembershipByPlanAndMethodBetweenDates(start, end)) {
            Long planId = row[0] instanceof Long id ? id : 0L;
            String planName = row[1] != null ? row[1].toString() : "Sin plan";
            PaymentMethod method = (PaymentMethod) row[2];
            long count = row[3] instanceof Number n ? n.longValue() : 0L;
            BigDecimal amount = row[4] instanceof BigDecimal b ? b : BigDecimal.ZERO;
            String key = planId + "|" + planName;
            PlanAggregate agg = aggregates.computeIfAbsent(key, k -> new PlanAggregate(planId, planName));
            agg.paymentCount += count;
            agg.totalAmount = agg.totalAmount.add(amount);
            if (BillingPaymentMethodRules.isBillable(method)) {
                agg.amountByMethod.merge(method, amount, BigDecimal::add);
            }
        }

        List<MembershipPlanReportLine> lines = new ArrayList<>();
        for (PlanAggregate agg : aggregates.values()) {
            lines.add(new MembershipPlanReportLine(
                    agg.planId > 0 ? agg.planId : null,
                    agg.planName,
                    agg.paymentCount,
                    agg.totalAmount,
                    agg.amountByMethod));
        }
        lines.sort(Comparator.comparing(MembershipPlanReportLine::planName, String.CASE_INSENSITIVE_ORDER));
        return lines;
    }

    private List<ProductSaleByPaymentLine> loadProductSalesByPayment(LocalDate start, LocalDate end) {
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endExclusive = end.plusDays(1).atStartOfDay();
        List<ProductSaleByPaymentLine> lines = new ArrayList<>();
        // Ventas normales de producto por medio de pago.
        for (Object[] row : saleRepository.aggregateByProductAndPaymentOnSaleDateBetween(startAt, endExclusive)) {
            long units = row[3] instanceof Number n ? n.longValue() : 0L;
            BigDecimal amount = row[4] instanceof BigDecimal b ? b : BigDecimal.ZERO;
            if (units <= 0 && amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            lines.add(new ProductSaleByPaymentLine(
                    (Long) row[0],
                    (String) row[1],
                    (PaymentMethod) row[2],
                    units,
                    amount));
        }

        // Abonos de fiado por producto/medio, visibles como filas separadas.
        for (Object[] row :
                productCreditPaymentRepository.aggregateByProductAndPaymentOnPaidAtBetween(startAt, endExclusive)) {
            BigDecimal amount = row[3] instanceof BigDecimal b ? b : BigDecimal.ZERO;
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            String productName = (String) row[1];
            lines.add(new ProductSaleByPaymentLine(
                    (Long) row[0],
                    "Abono fiado · " + productName,
                    (PaymentMethod) row[2],
                    0L,
                    amount));
        }

        lines.sort(Comparator.comparing(ProductSaleByPaymentLine::productName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ProductSaleByPaymentLine::paymentMethod));
        return lines;
    }

    private PeriodTotals loadPeriodTotals(LocalDate start, LocalDate end) {
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endExclusive = end.plusDays(1).atStartOfDay();
        Map<PaymentMethod, BigDecimal> billingIncomeByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        billingPaymentRepository.sumByPaymentMethodBetweenDates(start, end));
        BigDecimal billingIncomeTotal = BillingPaymentMethodTotals.sum(billingIncomeByMethod);
        long billingPaymentCount = billingPaymentRepository.countBetweenDatesExcludingPending(start, end);

        Map<PaymentMethod, BigDecimal> productSalesByMethod;
        long productSaleCount;
        long productUnitsSold;
        BigDecimal productSalesTotal;

        productSalesByMethod = BillingPaymentMethodTotals.fromAmountRows(
                saleRepository.sumBySaleDateBetweenGroupByPaymentMethod(startAt, endExclusive));
        productSaleCount = saleRepository.countSalesBySaleDateBetween(startAt, endExclusive);
        productUnitsSold = saleRepository.sumQuantityBySaleDateBetween(startAt, endExclusive);
        productSalesTotal = saleRepository.sumTotalAmountBySaleDateBetween(startAt, endExclusive);
        if (productSalesTotal == null) {
            productSalesTotal = BigDecimal.ZERO;
        }
        ProductSalesReportSection productSales = new ProductSalesReportSection(
                productSaleCount, productUnitsSold, productSalesTotal, productSalesByMethod);

        BigDecimal fiadoTotal;
        Map<PaymentMethod, BigDecimal> fiadoByMethod;
        fiadoTotal = productCreditPaymentRepository.sumAmountByPaidAtBetween(startAt, endExclusive);
        fiadoByMethod = BillingPaymentMethodTotals.fromAmountRows(
                productCreditPaymentRepository.sumByPaidAtBetweenGroupByPaymentMethod(startAt, endExclusive));
        if (fiadoTotal == null) {
            fiadoTotal = BigDecimal.ZERO;
        }

        Map<PaymentMethod, BigDecimal> expensesByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        expenseRepository.sumByPaymentMethodBetweenDates(start, end));
        BigDecimal expensesTotal = BillingPaymentMethodTotals.sum(expensesByMethod);
        long expenseCount = expenseRepository.countBetweenDates(start, end);

        Map<PaymentMethod, BigDecimal> otherIncomesByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        otherIncomeRepository.sumByPaymentMethodBetweenDates(start, end));
        Map<PaymentMethod, BigDecimal> totalIncomeByMethod = BillingPaymentMethodTotals.mergeAll(
                billingIncomeByMethod, productSalesByMethod, fiadoByMethod, otherIncomesByMethod);
        BigDecimal totalIncome = BillingPaymentMethodTotals.sum(totalIncomeByMethod);
        BigDecimal netResult = totalIncome.subtract(expensesTotal);

        return new PeriodTotals(
                billingPaymentCount,
                billingIncomeTotal,
                billingIncomeByMethod,
                productSales,
                fiadoTotal,
                fiadoByMethod,
                expenseCount,
                expensesTotal,
                expensesByMethod,
                totalIncome,
                totalIncomeByMethod,
                netResult);
    }

    private CashRegisterSnapshot resolveCashRegister(LocalDate date) {
        return cashRegisterRepository
                .findFirstByRegisterDateOrderByOpenedAtDesc(date)
                .map(reg -> new CashRegisterSnapshot(
                        reg.getStatus().name(),
                        reg.getOpeningCashAmount() != null ? reg.getOpeningCashAmount() : BigDecimal.ZERO))
                .orElseGet(() -> new CashRegisterSnapshot("NONE", BigDecimal.ZERO));
    }

    private List<ProductInventoryReportLine> buildInventoryForSingleDate(LocalDate date) {
        LocalDateTime startAt = date.atStartOfDay();
        LocalDateTime endExclusive = date.plusDays(1).atStartOfDay();
        return buildInventoryFromRows(saleRepository.aggregateQuantityByProductOnSaleDateBetween(startAt, endExclusive));
    }

    private List<ProductInventoryReportLine> buildInventoryForDateRange(LocalDate start, LocalDate end) {
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endExclusive = end.plusDays(1).atStartOfDay();
        return buildInventoryFromRows(
                saleRepository.aggregateQuantityByProductOnSaleDateBetween(startAt, endExclusive));
    }

    private List<ProductInventoryReportLine> buildInventoryFromRows(List<Object[]> salesRows) {
        Map<Long, long[]> salesByProduct = new HashMap<>();
        Map<Long, BigDecimal> amountByProduct = new HashMap<>();
        for (Object[] row : salesRows) {
            Long productId = (Long) row[0];
            long units = row[1] instanceof Number n ? n.longValue() : 0L;
            BigDecimal amount = row[2] instanceof BigDecimal b ? b : BigDecimal.ZERO;
            salesByProduct.put(productId, new long[] {units});
            amountByProduct.put(productId, amount);
        }

        List<ProductInventoryReportLine> lines = new ArrayList<>();
        for (Product product : productRepository.findByActiveTrueOrderByNameAsc()) {
            long unitsSold = 0L;
            BigDecimal salesAmount = BigDecimal.ZERO;
            long[] sold = salesByProduct.get(product.getId());
            if (sold != null) {
                unitsSold = sold[0];
                salesAmount = amountByProduct.getOrDefault(product.getId(), BigDecimal.ZERO);
            }
            int qty = product.getQuantity() != null ? product.getQuantity() : 0;
            int min = product.getMinStock() != null ? product.getMinStock() : 0;
            lines.add(new ProductInventoryReportLine(
                    product.getId(),
                    product.getName(),
                    product.getCategory(),
                    qty,
                    min,
                    qty <= min,
                    unitsSold,
                    salesAmount));
        }

        lines.sort(Comparator.comparing(ProductInventoryReportLine::lowStock)
                .reversed()
                .thenComparing(ProductInventoryReportLine::name, String.CASE_INSENSITIVE_ORDER));
        return lines;
    }

    private record CashRegisterSnapshot(String status, BigDecimal openingCashAmount) {}

    private static final class PlanAggregate {
        final Long planId;
        final String planName;
        long paymentCount;
        BigDecimal totalAmount = BigDecimal.ZERO;
        final Map<PaymentMethod, BigDecimal> amountByMethod = BillingPaymentMethodTotals.emptyBillableMap();

        PlanAggregate(Long planId, String planName) {
            this.planId = planId;
            this.planName = planName;
        }
    }

    private record PeriodTotals(
            long billingPaymentCount,
            BigDecimal billingIncomeTotal,
            Map<PaymentMethod, BigDecimal> billingIncomeByMethod,
            ProductSalesReportSection productSales,
            BigDecimal fiadoTotal,
            Map<PaymentMethod, BigDecimal> fiadoByMethod,
            long expenseCount,
            BigDecimal expensesTotal,
            Map<PaymentMethod, BigDecimal> expensesByMethod,
            BigDecimal totalIncome,
            Map<PaymentMethod, BigDecimal> totalIncomeByMethod,
            BigDecimal netResult) {}
}
