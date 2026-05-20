package com.gym.management.service;

import com.gym.management.dto.BatchSaleRequest;
import com.gym.management.dto.PaymentMethodTotals;
import com.gym.management.dto.ProductSalesRowResponse;
import com.gym.management.dto.SaleRequest;
import com.gym.management.dto.SaleResponse;
import com.gym.management.dto.SalesSummaryResponse;
import com.gym.management.dto.ShiftDetailResponse;
import com.gym.management.dto.WorkShiftResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.SaleMapper;
import com.gym.management.model.Employee;
import com.gym.management.model.PaymentMethod;
import com.gym.management.model.Product;
import com.gym.management.model.Sale;
import com.gym.management.model.ShiftStatus;
import com.gym.management.model.WorkShift;
import com.gym.management.repository.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final EmployeeService employeeService;
    private final ProductService productService;
    private final WorkShiftService workShiftService;

    @Transactional(readOnly = true)
    public List<SaleResponse> findAll(Long employeeId, Long workShiftId) {
        List<Sale> sales;
        if (workShiftId != null && employeeId != null) {
            sales = saleRepository.findByWorkShiftIdAndEmployeeIdOrderBySaleDateDescCreatedAtDesc(
                    workShiftId, employeeId);
        } else if (workShiftId != null) {
            sales = saleRepository.findByWorkShiftIdOrderBySaleDateDescCreatedAtDesc(workShiftId);
        } else if (employeeId != null) {
            sales = saleRepository.findByEmployeeIdOrderBySaleDateDescCreatedAtDesc(employeeId);
        } else {
            sales = saleRepository.findAllByOrderBySaleDateDescCreatedAtDesc();
        }
        return sales.stream().map(SaleMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse getSummary(Long workShiftId) {
        Map<PaymentMethod, BigDecimal> byMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            BigDecimal amount = workShiftId != null
                    ? saleRepository.sumTotalByPaymentMethodAndShift(method, workShiftId)
                    : saleRepository.sumTotalByPaymentMethod(method);
            byMethod.put(method, amount);
        }
        long totalSales = workShiftId != null
                ? saleRepository.countByShift(workShiftId)
                : saleRepository.count();
        long totalUnits = workShiftId != null
                ? saleRepository.sumTotalQuantityByShift(workShiftId)
                : saleRepository.sumTotalQuantity();
        BigDecimal totalAmount = workShiftId != null
                ? saleRepository.sumTotalAmountByShift(workShiftId)
                : saleRepository.sumTotalAmount();

        return new SalesSummaryResponse(totalSales, totalUnits, totalAmount, byMethod);
    }

    @Transactional(readOnly = true)
    public ShiftDetailResponse getShiftDetail(Long shiftId) {
        WorkShift shift = workShiftService.getShift(shiftId);
        SalesSummaryResponse summary = getSummary(shiftId);
        List<SaleResponse> sales = findAll(null, shiftId);
        return new ShiftDetailResponse(
                workShiftService.findById(shiftId),
                summary,
                buildProductRows(shiftId),
                sales);
    }

    @Transactional
    public List<SaleResponse> createBatch(BatchSaleRequest request) {
        WorkShift shift = workShiftService.getShift(request.workShiftId());
        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new BusinessException("El turno está cerrado. Abra un turno para registrar ventas.");
        }
        Employee employee = shift.getEmployee();
        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new BusinessException("El vendedor del turno no está activo");
        }

        Map<Long, Integer> quantityByProduct = new HashMap<>();
        for (var line : request.lines()) {
            quantityByProduct.merge(line.productId(), line.quantity(), Integer::sum);
        }
        for (var entry : quantityByProduct.entrySet()) {
            Product product = productService.getProduct(entry.getKey());
            if (product.getQuantity() < entry.getValue()) {
                throw new BusinessException(
                        "Stock insuficiente para " + product.getName() + ". Disponible: " + product.getQuantity());
            }
        }

        List<SaleResponse> created = new ArrayList<>();
        LocalDateTime saleDate = LocalDateTime.now();
        for (var line : request.lines()) {
            Product product = productService.getProduct(line.productId());
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BusinessException("El producto " + product.getName() + " no está activo");
            }
            BigDecimal unitPrice = product.getUnitPrice();
            BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(line.quantity()));
            product.setQuantity(product.getQuantity() - line.quantity());

            Sale sale = Sale.builder()
                    .workShift(shift)
                    .employee(employee)
                    .product(product)
                    .quantity(line.quantity())
                    .unitPrice(unitPrice)
                    .totalAmount(totalAmount)
                    .paymentMethod(line.paymentMethod())
                    .saleDate(saleDate)
                    .build();
            created.add(SaleMapper.toResponse(saleRepository.save(sale)));
        }
        return created;
    }

    @Transactional
    public SaleResponse create(SaleRequest request) {
        WorkShift shift = workShiftService.getShift(request.workShiftId());
        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new BusinessException("El turno está cerrado. Abra un turno para registrar ventas.");
        }

        Employee employee = shift.getEmployee();
        if (request.employeeId() != null && !request.employeeId().equals(employee.getId())) {
            throw new BusinessException("Las ventas del turno deben registrarse a nombre del vendedor del turno");
        }
        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new BusinessException("El empleado no está activo");
        }

        Product product = productService.getProduct(request.productId());
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BusinessException("El producto no está activo");
        }
        if (product.getQuantity() < request.quantity()) {
            throw new BusinessException(
                    "Stock insuficiente. Disponible: " + product.getQuantity());
        }

        BigDecimal unitPrice = product.getUnitPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));

        product.setQuantity(product.getQuantity() - request.quantity());

        LocalDateTime saleDate = LocalDateTime.now();
        if (request.saleDate() != null) {
            if (!java.time.LocalDate.now().equals(request.saleDate().toLocalDate())) {
                throw new BusinessException("Las ventas solo se pueden registrar en el día actual");
            }
            saleDate = request.saleDate();
        }

        Sale sale = Sale.builder()
                .workShift(shift)
                .employee(employee)
                .product(product)
                .quantity(request.quantity())
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .paymentMethod(request.paymentMethod())
                .saleDate(saleDate)
                .notes(request.notes())
                .build();

        return SaleMapper.toResponse(saleRepository.save(sale));
    }

    private List<ProductSalesRowResponse> buildProductRows(Long shiftId) {
        Map<Long, MutableProductRow> rows = new LinkedHashMap<>();
        for (Object[] row : saleRepository.aggregateByProductAndPayment(shiftId)) {
            Long productId = (Long) row[0];
            String productName = (String) row[1];
            PaymentMethod method = (PaymentMethod) row[2];
            int qty = ((Number) row[3]).intValue();
            BigDecimal amount = (BigDecimal) row[4];

            MutableProductRow mutable =
                    rows.computeIfAbsent(productId, id -> new MutableProductRow(productId, productName));
            mutable.add(method, qty, amount);
        }
        return rows.values().stream().map(MutableProductRow::toResponse).toList();
    }

    private static final class MutableProductRow {
        private final Long productId;
        private final String productName;
        private final Map<PaymentMethod, PaymentMethodTotals> byMethod = new EnumMap<>(PaymentMethod.class);
        private BigDecimal unitPrice = BigDecimal.ZERO;
        private int totalQty;
        private BigDecimal totalAmount = BigDecimal.ZERO;

        private MutableProductRow(Long productId, String productName) {
            this.productId = productId;
            this.productName = productName;
        }

        private void add(PaymentMethod method, int qty, BigDecimal amount) {
            byMethod.put(method, new PaymentMethodTotals(qty, amount));
            totalQty += qty;
            totalAmount = totalAmount.add(amount);
            if (unitPrice.equals(BigDecimal.ZERO) && qty > 0 && amount.compareTo(BigDecimal.ZERO) > 0) {
                unitPrice = amount.divide(BigDecimal.valueOf(qty), 2, java.math.RoundingMode.HALF_UP);
            }
        }

        private ProductSalesRowResponse toResponse() {
            return new ProductSalesRowResponse(
                    productId, productName, unitPrice, Map.copyOf(byMethod), totalQty, totalAmount);
        }
    }

    @Transactional
    public void delete(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + id));

        Product product = sale.getProduct();
        product.setQuantity(product.getQuantity() + sale.getQuantity());
        saleRepository.delete(sale);
    }
}
