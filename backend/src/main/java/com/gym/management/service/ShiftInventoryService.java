package com.gym.management.service;

import com.gym.management.dto.CashShortfallResponse;
import com.gym.management.dto.InventoryMissingLineDto;
import com.gym.management.dto.ProductInventoryCountItem;
import com.gym.management.dto.ProductInventoryLineResponse;
import com.gym.management.dto.CashDenominationCount;
import com.gym.management.dto.ShiftOpenCashPreviewResponse;
import com.gym.management.dto.ShiftOpenInventoryPreviewResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.model.BillingCashRegister;
import com.gym.management.model.BillingPaymentType;
import com.gym.management.model.CashShortfallKind;
import com.gym.management.model.Employee;
import com.gym.management.model.PaymentMethod;
import com.gym.management.model.Product;
import com.gym.management.model.ShiftStatus;
import com.gym.management.model.WorkShift;
import com.gym.management.repository.BillingCashRegisterExpenseRepository;
import com.gym.management.repository.BillingCashRegisterOtherIncomeRepository;
import com.gym.management.repository.BillingCashRegisterRepository;
import com.gym.management.repository.EmployeeCashShortfallRepository;
import com.gym.management.repository.BillingPaymentRepository;
import com.gym.management.repository.ProductCreditPaymentRepository;
import com.gym.management.repository.ProductRepository;
import com.gym.management.repository.SaleRepository;
import com.gym.management.repository.WorkShiftRepository;
import com.gym.management.util.MoneyUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftInventoryService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");
    private static final Set<CashShortfallKind> CASH_DRAWER_SHORTFALL_KINDS = EnumSet.of(
            CashShortfallKind.CASH_HANDOVER, CashShortfallKind.CASH_REGISTER, CashShortfallKind.CASH_SHIFT_OPEN);

    private final WorkShiftRepository workShiftRepository;
    private final ProductRepository productRepository;
    private final BillingCashRegisterRepository cashRegisterRepository;
    private final BillingPaymentRepository billingPaymentRepository;
    private final BillingCashRegisterExpenseRepository expenseRepository;
    private final BillingCashRegisterOtherIncomeRepository otherIncomeRepository;
    private final SaleRepository saleRepository;
    private final ProductCreditPaymentRepository productCreditPaymentRepository;
    private final CashShortfallService cashShortfallService;
    private final EmployeeCashShortfallRepository shortfallRepository;

    @Transactional(readOnly = true)
    public ShiftOpenInventoryPreviewResponse preview(LocalDate shiftDate) {
        LocalDate date = shiftDate != null ? shiftDate : LocalDate.now(GYM_ZONE);
        boolean required = isInventoryCheckRequired(date);
        List<ProductInventoryLineResponse> products = activeProductLines();
        if (!required) {
            return new ShiftOpenInventoryPreviewResponse(false, null, null, null, products, null);
        }
        WorkShift previous = getPreviousClosedShiftRequired(date);
        String employeeName = previous.getEmployee().getFirstName() + " " + previous.getEmployee().getLastName();
        ShiftOpenCashPreviewResponse cash = buildShiftOpenCashPreview(date);
        return new ShiftOpenInventoryPreviewResponse(
                true,
                previous.getId(),
                previous.getName(),
                employeeName,
                products,
                cash);
    }

    @Transactional(readOnly = true)
    public boolean isInventoryCheckRequired(LocalDate shiftDate) {
        return workShiftRepository.countByShiftDate(shiftDate) > 0;
    }

    @Transactional
    public InventoryOpenResult processBeforeOpen(
            LocalDate shiftDate, List<ProductInventoryCountItem> counts, CashDenominationCount cashCount) {
        if (!isInventoryCheckRequired(shiftDate)) {
            return InventoryOpenResult.none();
        }
        WorkShift previousShift = getPreviousClosedShiftRequired(shiftDate);
        Optional<CashShortfallResponse> cashShortfall = processCashBeforeOpen(shiftDate, cashCount, previousShift);

        List<Product> activeProducts = productRepository.findByActiveTrueOrderByNameAsc();
        if (activeProducts.isEmpty()) {
            return new InventoryOpenResult(false, null, BigDecimal.ZERO, List.of(), cashShortfall.orElse(null));
        }
        if (counts == null || counts.isEmpty()) {
            throw new BusinessException(
                    "Debe confirmar el inventario de todos los productos antes de abrir el turno");
        }
        Map<Long, Integer> countedByProduct = new HashMap<>();
        for (ProductInventoryCountItem item : counts) {
            if (item.productId() == null || item.countedQuantity() == null) {
                throw new BusinessException("Cada producto debe tener cantidad contada");
            }
            if (countedByProduct.put(item.productId(), item.countedQuantity()) != null) {
                throw new BusinessException("Producto duplicado en el conteo de inventario");
            }
        }
        for (Product product : activeProducts) {
            if (!countedByProduct.containsKey(product.getId())) {
                throw new BusinessException("Falta el conteo del producto: " + product.getName());
            }
        }

        List<InventoryMissingLineDto> missingLines = new ArrayList<>();
        BigDecimal shortfallTotal = BigDecimal.ZERO;

        for (Product product : activeProducts) {
            int expected = product.getQuantity();
            int counted = countedByProduct.get(product.getId());
            if (counted < 0) {
                throw new BusinessException("Cantidad inválida para " + product.getName());
            }
            product.setQuantity(counted);
            int missing = expected - counted;
            if (missing > 0) {
                BigDecimal lineValue = MoneyUtil.roundPesos(
                        product.getUnitPrice().multiply(BigDecimal.valueOf(missing)));
                shortfallTotal = shortfallTotal.add(lineValue);
                missingLines.add(new InventoryMissingLineDto(
                        product.getId(),
                        product.getName(),
                        product.getCategory(),
                        expected,
                        counted,
                        missing,
                        product.getUnitPrice(),
                        lineValue));
            }
        }
        productRepository.saveAll(activeProducts);
        shortfallTotal = MoneyUtil.roundPesos(shortfallTotal);

        Optional<CashShortfallResponse> inventoryShortfall = Optional.empty();
        if (shortfallTotal.compareTo(BigDecimal.ZERO) > 0) {
            inventoryShortfall = Optional.of(cashShortfallService.registerFromInventoryCheck(
                    previousShift, shortfallTotal, missingLines));
        }

        return new InventoryOpenResult(
                true,
                inventoryShortfall.orElse(null),
                shortfallTotal,
                missingLines,
                cashShortfall.orElse(null));
    }

    private Optional<CashShortfallResponse> processCashBeforeOpen(
            LocalDate shiftDate, CashDenominationCount cashCount, WorkShift previousShift) {
        if (cashCount == null) {
            throw new BusinessException("Debe contar el efectivo en caja antes de abrir el turno");
        }
        ShiftOpenCashPreviewResponse preview = buildShiftOpenCashPreview(shiftDate);
        BigDecimal expected = preview.expectedCashTotal();
        BigDecimal declared = MoneyUtil.roundPesos(cashCount.totalCash());
        BillingCashRegister register = cashRegisterRepository
                .findByIdWithEmployee(preview.cashRegisterId())
                .orElseThrow(() -> new BusinessException("Caja del día no encontrada"));
        return cashShortfallService.registerFromShiftOpenCashCheck(
                register, previousShift, expected, declared);
    }

    @Transactional
    public InventoryCloseAtRegisterResult processAtCashRegisterClose(
            LocalDate registerDate,
            List<ProductInventoryCountItem> counts,
            Employee responsible,
            WorkShift referenceShift) {
        List<Product> activeProducts = productRepository.findByActiveTrueOrderByNameAsc();
        if (activeProducts.isEmpty()) {
            return InventoryCloseAtRegisterResult.none();
        }
        if (counts == null || counts.isEmpty()) {
            throw new BusinessException("Debe confirmar el conteo de todos los productos al cerrar la caja");
        }
        Map<Long, Integer> countedByProduct = new HashMap<>();
        for (ProductInventoryCountItem item : counts) {
            if (item.productId() == null || item.countedQuantity() == null) {
                throw new BusinessException("Cada producto debe tener cantidad contada");
            }
            if (countedByProduct.put(item.productId(), item.countedQuantity()) != null) {
                throw new BusinessException("Producto duplicado en el conteo de inventario");
            }
        }
        for (Product product : activeProducts) {
            if (!countedByProduct.containsKey(product.getId())) {
                throw new BusinessException("Falta el conteo del producto: " + product.getName());
            }
        }

        List<InventoryMissingLineDto> missingLines = new ArrayList<>();
        BigDecimal shortfallTotal = BigDecimal.ZERO;

        for (Product product : activeProducts) {
            int expected = product.getQuantity();
            int counted = countedByProduct.get(product.getId());
            if (counted < 0) {
                throw new BusinessException("Cantidad inválida para " + product.getName());
            }
            product.setQuantity(counted);
            int missing = expected - counted;
            if (missing > 0) {
                BigDecimal lineValue = MoneyUtil.roundPesos(
                        product.getUnitPrice().multiply(BigDecimal.valueOf(missing)));
                shortfallTotal = shortfallTotal.add(lineValue);
                missingLines.add(new InventoryMissingLineDto(
                        product.getId(),
                        product.getName(),
                        product.getCategory(),
                        expected,
                        counted,
                        missing,
                        product.getUnitPrice(),
                        lineValue));
            }
        }
        productRepository.saveAll(activeProducts);
        shortfallTotal = MoneyUtil.roundPesos(shortfallTotal);

        Optional<CashShortfallResponse> shortfall = cashShortfallService.registerFromCashRegisterInventory(
                responsible, referenceShift, registerDate, shortfallTotal, missingLines);

        return new InventoryCloseAtRegisterResult(shortfall.orElse(null), shortfallTotal, missingLines);
    }

    public List<ProductInventoryLineResponse> listActiveProductLines() {
        return activeProductLines();
    }

    private List<ProductInventoryLineResponse> activeProductLines() {
        return productRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(p -> new ProductInventoryLineResponse(
                        p.getId(),
                        p.getName(),
                        p.getCategory(),
                        p.getQuantity(),
                        p.getUnitPrice()))
                .toList();
    }

    private WorkShift getPreviousClosedShiftRequired(LocalDate shiftDate) {
        return workShiftRepository
                .findFirstByShiftDateAndStatusOrderByClosedAtDesc(shiftDate, ShiftStatus.CLOSED)
                .orElseThrow(() -> new BusinessException(
                        "No se encontró el turno anterior cerrado del día para validar inventario"));
    }

    private ShiftOpenCashPreviewResponse buildShiftOpenCashPreview(LocalDate shiftDate) {
        LocalDate date = shiftDate != null ? shiftDate : LocalDate.now(GYM_ZONE);
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
        BigDecimal productCash = saleRepository.sumCashAmountByShiftDate(date);
        if (productCash == null) {
            productCash = BigDecimal.ZERO;
        }
        Map<BillingPaymentType, BigDecimal> cashByType = loadCashByBillingType(id);
        BigDecimal fiadoCash = sumFiadoCashCollected(date);
        Map<PaymentMethod, BigDecimal> otherIncomesByMethod =
                BillingPaymentMethodTotals.fromAmountRows(
                        otherIncomeRepository.sumByPaymentMethodByCashRegisterId(id));
        BigDecimal otherIncomesCash =
                otherIncomesByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        BigDecimal systemCash = computeCashInDrawer(register, true);
        BigDecimal deducted = sumRegisteredCashShortfallsForDate(date);
        BigDecimal expected = netCashExpectedAfterShortfalls(systemCash, deducted);
        Employee opener = register.getOpenedBy();
        String openerName = opener.getFirstName() + " " + opener.getLastName();
        return new ShiftOpenCashPreviewResponse(
                id,
                openerName,
                register.getOpeningCashAmount(),
                cashExpenses,
                productCash,
                fiadoCash,
                cashByType.getOrDefault(BillingPaymentType.MEMBERSHIP, BigDecimal.ZERO),
                cashByType.getOrDefault(BillingPaymentType.DAY_WORKOUT, BigDecimal.ZERO),
                cashByType.getOrDefault(BillingPaymentType.SPORTS_DANCE, BigDecimal.ZERO),
                MoneyUtil.roundPesos(otherIncomesCash),
                systemCash,
                deducted,
                expected);
    }

    private BigDecimal sumRegisteredCashShortfallsForDate(LocalDate date) {
        BigDecimal sum = shortfallRepository.sumShortfallAmountByRecordDateAndKinds(date, CASH_DRAWER_SHORTFALL_KINDS);
        return sum != null ? MoneyUtil.roundPesos(sum) : BigDecimal.ZERO;
    }

    private static BigDecimal netCashExpectedAfterShortfalls(BigDecimal systemCash, BigDecimal deducted) {
        BigDecimal net = MoneyUtil.roundPesos(systemCash.subtract(deducted));
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
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
        BigDecimal sum = productCreditPaymentRepository.sumAmountByShiftDateAndMethod(date, PaymentMethod.CASH);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public record InventoryOpenResult(
            boolean adjusted,
            CashShortfallResponse inventoryShortfall,
            BigDecimal inventoryShortfallAmount,
            List<InventoryMissingLineDto> missingLines,
            CashShortfallResponse cashShortfall) {

        static InventoryOpenResult none() {
            return new InventoryOpenResult(false, null, BigDecimal.ZERO, List.of(), null);
        }
    }

    public record InventoryCloseAtRegisterResult(
            CashShortfallResponse shortfall,
            BigDecimal shortfallAmount,
            List<InventoryMissingLineDto> missingLines) {

        static InventoryCloseAtRegisterResult none() {
            return new InventoryCloseAtRegisterResult(null, BigDecimal.ZERO, List.of());
        }
    }
}
