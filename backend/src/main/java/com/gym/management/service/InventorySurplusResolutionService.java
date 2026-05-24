package com.gym.management.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.dto.InventoryMissingLineDto;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.CashShortfallKind;
import com.gym.management.model.CashShortfallStatus;
import com.gym.management.model.Employee;
import com.gym.management.model.EmployeeCashShortfall;
import com.gym.management.model.Product;
import com.gym.management.repository.EmployeeCashShortfallRepository;
import com.gym.management.repository.ProductRepository;
import com.gym.management.util.MoneyUtil;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuando en entrega de turno hay sobrante de efectivo, intenta cuadrarlo con faltantes de
 * inventario pendientes del mismo empleado y día: ajusta stock y liquida el descuadre sin cobrar
 * faltante de caja en la entrega.
 */
@Service
@RequiredArgsConstructor
public class InventorySurplusResolutionService {

    private static final TypeReference<List<InventoryMissingLineDto>> INVENTORY_LINES_TYPE =
            new TypeReference<>() {};

    private final EmployeeCashShortfallRepository shortfallRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    /**
     * @return mensaje para mostrar al usuario si el sobrante coincidió con inventario faltante
     */
    @Transactional
    public Optional<String> tryResolveHandoverSurplus(
            Employee employee, LocalDate shiftDate, BigDecimal surplusAmount, Employee settledBy) {
        BigDecimal surplus = MoneyUtil.roundPesos(surplusAmount);
        if (surplus.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        List<EmployeeCashShortfall> pending = shortfallRepository.findPendingInventoryByEmployeeAndDate(
                employee.getId(), shiftDate, CashShortfallStatus.PENDING, CashShortfallKind.INVENTORY);
        if (pending.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal inventoryTotal = pending.stream()
                .map(EmployeeCashShortfall::getShortfallAmount)
                .map(MoneyUtil::roundPesos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        inventoryTotal = MoneyUtil.roundPesos(inventoryTotal);
        if (inventoryTotal.compareTo(surplus) != 0) {
            return Optional.empty();
        }

        Map<Long, Integer> unitsToDeduct = aggregateMissingUnits(pending);
        applyInventoryAdjustments(unitsToDeduct);
        settleInventoryShortfalls(pending, settledBy, surplus);

        int totalUnits = unitsToDeduct.values().stream().mapToInt(Integer::intValue).sum();
        return Optional.of(
                "Sobrante de "
                        + MoneyUtil.formatPesos(surplus)
                        + " cuadrado con inventario faltante ("
                        + totalUnits
                        + " unidad"
                        + (totalUnits == 1 ? "" : "es")
                        + "). Stock actualizado; no se registró descuadre de entrega.");
    }

    private Map<Long, Integer> aggregateMissingUnits(List<EmployeeCashShortfall> shortfalls) {
        Map<Long, Integer> byProduct = new HashMap<>();
        for (EmployeeCashShortfall shortfall : shortfalls) {
            for (InventoryMissingLineDto line : parseInventoryJson(shortfall.getInventoryMissingJson())) {
                Long productId = resolveProductId(line);
                if (productId == null) {
                    throw new BusinessException(
                            "No se pudo identificar el producto «" + line.productName() + "» para ajustar inventario");
                }
                byProduct.merge(productId, line.missingQuantity(), Integer::sum);
            }
        }
        return byProduct;
    }

    private void applyInventoryAdjustments(Map<Long, Integer> unitsToDeduct) {
        for (Map.Entry<Long, Integer> entry : unitsToDeduct.entrySet()) {
            Product product = productRepository
                    .findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + entry.getKey()));
            int missing = entry.getValue();
            int newQty = product.getQuantity() - missing;
            if (newQty < 0) {
                throw new BusinessException(
                        "No hay stock suficiente de «" + product.getName() + "» para registrar la venta del faltante");
            }
            product.setQuantity(newQty);
            productRepository.save(product);
        }
    }

    private void settleInventoryShortfalls(
            List<EmployeeCashShortfall> shortfalls, Employee settledBy, BigDecimal surplus) {
        Instant now = Instant.now();
        String autoNote =
                "Cuadrado automáticamente: sobrante en entrega de turno ("
                        + MoneyUtil.formatPesos(surplus)
                        + ") igual al valor de productos faltantes.";
        for (EmployeeCashShortfall record : shortfalls) {
            record.setStatus(CashShortfallStatus.SETTLED);
            record.setSettledAt(now);
            record.setSettledBy(settledBy);
            String existing = record.getNotes();
            record.setNotes(existing != null && !existing.isBlank() ? existing + " · " + autoNote : autoNote);
            shortfallRepository.save(record);
        }
    }

    private Long resolveProductId(InventoryMissingLineDto line) {
        if (line.productId() != null) {
            return line.productId();
        }
        return productRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(p -> p.getName().equalsIgnoreCase(line.productName()))
                .map(Product::getId)
                .findFirst()
                .orElse(null);
    }

    private List<InventoryMissingLineDto> parseInventoryJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<InventoryMissingLineDto> lines = objectMapper.readValue(json, INVENTORY_LINES_TYPE);
            return lines != null ? lines : List.of();
        } catch (Exception e) {
            throw new BusinessException("No se pudo leer el detalle de inventario faltante");
        }
    }
}
