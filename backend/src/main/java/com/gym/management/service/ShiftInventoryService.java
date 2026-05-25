package com.gym.management.service;

import com.gym.management.dto.CashShortfallResponse;
import com.gym.management.dto.HandoverInventoryResult;
import com.gym.management.dto.InventoryMissingLineDto;
import com.gym.management.dto.InventorySurplusLineDto;
import com.gym.management.dto.ProductInventoryCountItem;
import com.gym.management.dto.ProductInventoryLineResponse;
import com.gym.management.dto.CashDenominationCount;
import com.gym.management.dto.BillingCashRegisterOtherIncomeResponse;
import com.gym.management.dto.ShiftOpenCashPreviewResponse;
import com.gym.management.dto.ShiftOpenInventoryPreviewResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.model.BillingCashRegister;
import com.gym.management.model.CashShortfallKind;
import com.gym.management.model.CashShortfallStatus;
import com.gym.management.model.Employee;
import com.gym.management.model.EmployeeCashShortfall;
import com.gym.management.model.PaymentMethod;
import com.gym.management.model.Product;
import com.gym.management.model.ShiftHandover;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftInventoryService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

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

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private BillingCashRegisterService billingCashRegisterService;

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
        ShiftOpenCashPreviewResponse cash = billingCashRegisterService.shiftOpenCashPreview(date);
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
        CashOpenCheckResult cashCheck = processCashBeforeOpen(shiftDate, cashCount, previousShift);

        List<Product> activeProducts = productRepository.findByActiveTrueOrderByNameAsc();
        if (activeProducts.isEmpty()) {
            return new InventoryOpenResult(
                    false,
                    null,
                    BigDecimal.ZERO,
                    List.of(),
                    cashCheck.cashShortfall(),
                    cashCheck.cashSurplusRegistered(),
                    cashCheck.cashSurplusAmount(),
                    cashCheck.cashSurplusBillingObservation());
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
                cashCheck.cashShortfall(),
                cashCheck.cashSurplusRegistered(),
                cashCheck.cashSurplusAmount(),
                cashCheck.cashSurplusBillingObservation());
    }

    private CashOpenCheckResult processCashBeforeOpen(
            LocalDate shiftDate, CashDenominationCount cashCount, WorkShift previousShift) {
        if (cashCount == null) {
            throw new BusinessException("Debe contar el efectivo en caja antes de abrir el turno");
        }
        ShiftOpenCashPreviewResponse preview = billingCashRegisterService.shiftOpenCashPreview(shiftDate);
        BigDecimal expected = preview.expectedCashTotal();
        BigDecimal declared = MoneyUtil.roundPesos(cashCount.totalCash());
        BillingCashRegister register = cashRegisterRepository
                .findByIdWithEmployee(preview.cashRegisterId())
                .orElseThrow(() -> new BusinessException("Caja del día no encontrada"));
        Optional<CashShortfallResponse> shortfall = cashShortfallService.registerFromShiftOpenCashCheck(
                register, previousShift, expected, declared);
        Optional<BillingCashRegisterOtherIncomeResponse> surplus =
                billingCashRegisterService.registerShiftOpenCashSurplus(
                        register, previousShift, expected, declared, previousShift.getEmployee());
        return new CashOpenCheckResult(
                shortfall.orElse(null),
                surplus.isPresent(),
                surplus.map(BillingCashRegisterOtherIncomeResponse::amount).orElse(BigDecimal.ZERO),
                surplus.map(BillingCashRegisterOtherIncomeResponse::observation).orElse(null));
    }

    private record CashOpenCheckResult(
            CashShortfallResponse cashShortfall,
            boolean cashSurplusRegistered,
            BigDecimal cashSurplusAmount,
            String cashSurplusBillingObservation) {}

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

    /**
     * Conteo de productos al entregar turno: actualiza stock, registra faltante de inventario y calcula
     * crédito por unidades sobrantes (valor de venta) para cruzar con descuadres pendientes.
     */
    @Transactional
    public HandoverInventoryResult processAtHandover(
            WorkShift handingOverShift, List<ProductInventoryCountItem> counts, ShiftHandover handover) {
        List<Product> activeProducts = productRepository.findByActiveTrueOrderByNameAsc();
        if (activeProducts.isEmpty()) {
            return new HandoverInventoryResult(
                    false, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(), null);
        }
        if (counts == null || counts.isEmpty()) {
            throw new BusinessException("Debe confirmar el conteo de todos los productos al entregar el turno");
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
        List<InventorySurplusLineDto> surplusLines = new ArrayList<>();
        BigDecimal shortfallTotal = BigDecimal.ZERO;
        BigDecimal surplusCredit = BigDecimal.ZERO;

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
            } else if (counted > expected) {
                int surplus = counted - expected;
                BigDecimal lineValue =
                        MoneyUtil.roundPesos(product.getUnitPrice().multiply(BigDecimal.valueOf(surplus)));
                surplusCredit = surplusCredit.add(lineValue);
                surplusLines.add(new InventorySurplusLineDto(
                        product.getId(),
                        product.getName(),
                        product.getCategory(),
                        expected,
                        counted,
                        surplus,
                        product.getUnitPrice(),
                        lineValue));
            }
        }
        productRepository.saveAll(activeProducts);
        shortfallTotal = MoneyUtil.roundPesos(shortfallTotal);
        surplusCredit = MoneyUtil.roundPesos(surplusCredit);

        Optional<CashShortfallResponse> newShortfall = Optional.empty();
        if (shortfallTotal.compareTo(BigDecimal.ZERO) > 0) {
            if (handover != null && handover.getId() != null) {
                newShortfall = Optional.of(cashShortfallService.registerFromInventoryCheckAtHandover(
                        handover, shortfallTotal, missingLines));
            } else {
                newShortfall = Optional.of(cashShortfallService.registerFromInventoryCheck(
                        handingOverShift, shortfallTotal, missingLines));
            }
        }

        return new HandoverInventoryResult(
                true,
                shortfallTotal,
                surplusCredit,
                missingLines,
                surplusLines,
                newShortfall.orElse(null));
    }

    @Transactional(readOnly = true)
    public BigDecimal sumPendingInventoryShortfallForEmployee(LocalDate shiftDate, Long employeeId) {
        List<EmployeeCashShortfall> pending = shortfallRepository.findPendingInventoryByEmployeeAndDate(
                employeeId, shiftDate, CashShortfallStatus.PENDING, CashShortfallKind.INVENTORY);
        return MoneyUtil.roundPesos(pending.stream()
                .map(EmployeeCashShortfall::getShortfallAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
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

    public record InventoryOpenResult(
            boolean adjusted,
            CashShortfallResponse inventoryShortfall,
            BigDecimal inventoryShortfallAmount,
            List<InventoryMissingLineDto> missingLines,
            CashShortfallResponse cashShortfall,
            boolean cashSurplusRegistered,
            BigDecimal cashSurplusAmount,
            String cashSurplusBillingObservation) {

        static InventoryOpenResult none() {
            return new InventoryOpenResult(
                    false, null, BigDecimal.ZERO, List.of(), null, false, BigDecimal.ZERO, null);
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
