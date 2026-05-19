package com.gym.management.service;

import com.gym.management.dto.SaleRequest;
import com.gym.management.dto.SaleResponse;
import com.gym.management.dto.SalesSummaryResponse;
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
import java.util.EnumMap;
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

    @Transactional
    public SaleResponse create(SaleRequest request) {
        WorkShift shift = workShiftService.getShift(request.workShiftId());
        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new BusinessException("El turno está cerrado. Abra un turno para registrar ventas.");
        }

        Employee employee = employeeService.getEmployee(request.employeeId());
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

    @Transactional
    public void delete(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + id));

        Product product = sale.getProduct();
        product.setQuantity(product.getQuantity() + sale.getQuantity());
        saleRepository.delete(sale);
    }
}
