package com.gym.management.service;

import com.gym.management.dto.CashShortfallResponse;
import com.gym.management.dto.InventoryMissingLineDto;
import com.gym.management.dto.ProductInventoryCountItem;
import com.gym.management.dto.ProductInventoryLineResponse;
import com.gym.management.dto.ShiftOpenInventoryPreviewResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.model.Employee;
import com.gym.management.model.Product;
import com.gym.management.model.ShiftStatus;
import com.gym.management.model.WorkShift;
import com.gym.management.repository.ProductRepository;
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
    private final CashShortfallService cashShortfallService;

    @Transactional(readOnly = true)
    public ShiftOpenInventoryPreviewResponse preview(LocalDate shiftDate) {
        LocalDate date = shiftDate != null ? shiftDate : LocalDate.now(GYM_ZONE);
        boolean required = isInventoryCheckRequired(date);
        List<ProductInventoryLineResponse> products = activeProductLines();
        if (!required) {
            return new ShiftOpenInventoryPreviewResponse(false, null, null, null, products);
        }
        WorkShift previous = getPreviousClosedShiftRequired(date);
        String employeeName = previous.getEmployee().getFirstName() + " " + previous.getEmployee().getLastName();
        return new ShiftOpenInventoryPreviewResponse(
                true,
                previous.getId(),
                previous.getName(),
                employeeName,
                products);
    }

    @Transactional(readOnly = true)
    public boolean isInventoryCheckRequired(LocalDate shiftDate) {
        return workShiftRepository.countByShiftDate(shiftDate) > 0;
    }

    @Transactional
    public InventoryOpenResult processBeforeOpen(LocalDate shiftDate, List<ProductInventoryCountItem> counts) {
        if (!isInventoryCheckRequired(shiftDate)) {
            return InventoryOpenResult.none();
        }
        if (counts == null || counts.isEmpty()) {
            throw new BusinessException(
                    "Debe confirmar el inventario de todos los productos antes de abrir el turno");
        }
        List<Product> activeProducts = productRepository.findByActiveTrueOrderByNameAsc();
        if (activeProducts.isEmpty()) {
            return InventoryOpenResult.none();
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

        WorkShift previousShift = getPreviousClosedShiftRequired(shiftDate);
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

        Optional<CashShortfallResponse> shortfall = Optional.empty();
        if (shortfallTotal.compareTo(BigDecimal.ZERO) > 0) {
            shortfall = Optional.of(cashShortfallService.registerFromInventoryCheck(
                    previousShift, shortfallTotal, missingLines));
        }

        return new InventoryOpenResult(true, shortfall.orElse(null), shortfallTotal, missingLines);
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

    public record InventoryOpenResult(
            boolean adjusted,
            CashShortfallResponse shortfall,
            BigDecimal shortfallAmount,
            List<InventoryMissingLineDto> missingLines) {

        static InventoryOpenResult none() {
            return new InventoryOpenResult(false, null, BigDecimal.ZERO, List.of());
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
