package com.gym.management.service;

import com.gym.management.dto.DigitalAccountIncomeLineResponse;
import com.gym.management.dto.DigitalAccountIncomeSource;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.BillingPaymentMapper;
import com.gym.management.mapper.SaleMapper;
import com.gym.management.model.BillingCashRegisterOtherIncome;
import com.gym.management.model.BillingPayment;
import com.gym.management.model.PaymentMethod;
import com.gym.management.model.Product;
import com.gym.management.model.ProductCredit;
import com.gym.management.model.ProductCreditPayment;
import com.gym.management.model.ProductCreditStatus;
import com.gym.management.model.Sale;
import com.gym.management.repository.BillingCashRegisterOtherIncomeRepository;
import com.gym.management.repository.BillingPaymentRepository;
import com.gym.management.repository.ProductCreditPaymentRepository;
import com.gym.management.repository.ProductCreditRepository;
import com.gym.management.repository.SaleRepository;
import com.gym.management.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DigitalAccountIncomeService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");
    private static final List<PaymentMethod> DIGITAL_METHODS = List.of(PaymentMethod.NEQUI, PaymentMethod.BANCOLOMBIA);
    private static final List<PaymentMethod> CASH_METHODS = List.of(PaymentMethod.CASH);

    private final BillingPaymentRepository billingPaymentRepository;
    private final BillingCashRegisterOtherIncomeRepository otherIncomeRepository;
    private final SaleRepository saleRepository;
    private final ProductCreditPaymentRepository productCreditPaymentRepository;
    private final ProductCreditRepository productCreditRepository;
    private final BillingService billingService;
    private final SaleService saleService;

    @Transactional(readOnly = true)
    public List<DigitalAccountIncomeLineResponse> listCurrentMonth() {
        requireSuperAdmin();
        LocalDate today = LocalDate.now(GYM_ZONE);
        return listBetween(today.withDayOfMonth(1), today, DIGITAL_METHODS);
    }

    @Transactional(readOnly = true)
    public List<DigitalAccountIncomeLineResponse> listCurrentMonthCash() {
        requireSuperAdmin();
        LocalDate today = LocalDate.now(GYM_ZONE);
        return listBetween(today.withDayOfMonth(1), today, CASH_METHODS);
    }

    @Transactional(readOnly = true)
    public List<DigitalAccountIncomeLineResponse> listBetween(
            LocalDate start, LocalDate end, List<PaymentMethod> methods) {
        requireSuperAdmin();
        List<DigitalAccountIncomeLineResponse> lines = new ArrayList<>();
        for (BillingPayment payment : billingPaymentRepository.findDigitalBetweenDates(start, end, methods)) {
            String memberName = payment.getMember() != null
                    ? payment.getMember().getFirstName() + " " + payment.getMember().getLastName()
                    : (payment.getGuestLabel() != null ? payment.getGuestLabel() : "Invitado");
            String desc = BillingPaymentMapper.paymentTypeLabel(payment.getPaymentType()) + " · " + memberName;
            lines.add(new DigitalAccountIncomeLineResponse(
                    DigitalAccountIncomeSource.BILLING_PAYMENT,
                    "Facturación",
                    payment.getId(),
                    payment.getPaymentMethod(),
                    SaleMapper.paymentMethodLabel(payment.getPaymentMethod()),
                    payment.getAmount(),
                    payment.getPaymentDate(),
                    payment.getCreatedAt(),
                    desc,
                    employeeName(payment.getEmployee())));
        }
        for (BillingCashRegisterOtherIncome income :
                otherIncomeRepository.findDigitalBetweenDates(start, end, methods)) {
            lines.add(new DigitalAccountIncomeLineResponse(
                    DigitalAccountIncomeSource.OTHER_INCOME,
                    "Otro ingreso",
                    income.getId(),
                    income.getPaymentMethod(),
                    SaleMapper.paymentMethodLabel(income.getPaymentMethod()),
                    income.getAmount(),
                    income.getCashRegister().getRegisterDate(),
                    income.getCreatedAt(),
                    income.getObservation(),
                    employeeName(income.getRecordedBy())));
        }
        LocalDateTime saleStart = start.atStartOfDay();
        LocalDateTime saleEndExclusive = end.plusDays(1).atStartOfDay();
        for (Sale sale : saleRepository.findDigitalBetweenDates(saleStart, saleEndExclusive, methods)) {
            String productName = sale.getProduct() != null ? sale.getProduct().getName() : "Producto";
            lines.add(new DigitalAccountIncomeLineResponse(
                    DigitalAccountIncomeSource.SALE,
                    "Venta producto",
                    sale.getId(),
                    sale.getPaymentMethod(),
                    SaleMapper.paymentMethodLabel(sale.getPaymentMethod()),
                    sale.getTotalAmount(),
                    sale.getSaleDate().toLocalDate(),
                    sale.getCreatedAt(),
                    productName + " × " + sale.getQuantity(),
                    employeeName(sale.getEmployee())));
        }
        for (ProductCreditPayment payment :
                productCreditPaymentRepository.findDigitalBetweenShiftDates(start, end, methods)) {
            ProductCredit credit = payment.getCredit();
            String memberName = credit.getMember().getFirstName() + " " + credit.getMember().getLastName();
            String productName = credit.getProduct() != null ? credit.getProduct().getName() : "Producto";
            lines.add(new DigitalAccountIncomeLineResponse(
                    DigitalAccountIncomeSource.PRODUCT_CREDIT,
                    "Abono fiado",
                    payment.getId(),
                    payment.getPaymentMethod(),
                    SaleMapper.paymentMethodLabel(payment.getPaymentMethod()),
                    payment.getAmount(),
                    payment.getWorkShift().getShiftDate(),
                    payment.getCreatedAt(),
                    productName + " · " + memberName,
                    employeeName(payment.getEmployee())));
        }
        lines.sort(Comparator.comparing(DigitalAccountIncomeLineResponse::createdAt).reversed());
        return lines;
    }

    @Transactional
    public void delete(DigitalAccountIncomeSource source, Long id) {
        delete(source, id, DIGITAL_METHODS, false);
    }

    @Transactional
    public void deleteCash(DigitalAccountIncomeSource source, Long id) {
        delete(source, id, CASH_METHODS, true);
    }

    private void delete(
            DigitalAccountIncomeSource source, Long id, List<PaymentMethod> methods, boolean cash) {
        requireSuperAdmin();
        LocalDate today = LocalDate.now(GYM_ZONE);
        LocalDate monthStart = today.withDayOfMonth(1);
        switch (source) {
            case BILLING_PAYMENT -> {
                if (cash) {
                    billingService.deleteCashPaymentInCurrentMonth(id, monthStart, today);
                } else {
                    billingService.deleteDigitalPaymentInCurrentMonth(id, monthStart, today);
                }
            }
            case OTHER_INCOME -> deleteOtherIncome(id, monthStart, today, methods);
            case SALE -> deleteSale(id, monthStart, today, methods);
            case PRODUCT_CREDIT -> deleteProductCreditPayment(id, monthStart, today, methods);
            default -> throw new BusinessException("Origen de ingreso no válido");
        }
    }

    private void deleteOtherIncome(Long id, LocalDate monthStart, LocalDate today, List<PaymentMethod> methods) {
        BillingCashRegisterOtherIncome income = otherIncomeRepository
                .findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingreso no encontrado: " + id));
        requirePaymentMethod(income.getPaymentMethod(), methods);
        LocalDate registerDate = income.getCashRegister().getRegisterDate();
        assertInCurrentMonth(registerDate, monthStart, today);
        otherIncomeRepository.delete(income);
    }

    private void deleteSale(Long id, LocalDate monthStart, LocalDate today, List<PaymentMethod> methods) {
        Sale sale = saleRepository
                .findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + id));
        requirePaymentMethod(sale.getPaymentMethod(), methods);
        LocalDate saleDate = sale.getSaleDate().toLocalDate();
        assertInCurrentMonth(saleDate, monthStart, today);
        saleService.delete(id);
    }

    private void deleteProductCreditPayment(
            Long id, LocalDate monthStart, LocalDate today, List<PaymentMethod> methods) {
        ProductCreditPayment payment = productCreditPaymentRepository
                .findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abono fiado no encontrado: " + id));
        requirePaymentMethod(payment.getPaymentMethod(), methods);
        LocalDate shiftDate = payment.getWorkShift().getShiftDate();
        assertInCurrentMonth(shiftDate, monthStart, today);
        ProductCredit credit = payment.getCredit();
        credit.setBalance(credit.getBalance().add(payment.getAmount()));
        if (credit.getStatus() == ProductCreditStatus.PAID) {
            credit.setStatus(ProductCreditStatus.OPEN);
        }
        productCreditRepository.save(credit);
        productCreditPaymentRepository.delete(payment);
    }

    private static void requireSuperAdmin() {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException("Solo el super administrador puede gestionar ingresos de Nequi y Bancolombia");
        }
    }

    private static void requirePaymentMethod(PaymentMethod method, List<PaymentMethod> allowed) {
        if (!allowed.contains(method)) {
            throw new BusinessException("El medio de pago del ingreso no coincide con el tipo solicitado");
        }
    }

    private static void assertInCurrentMonth(LocalDate date, LocalDate monthStart, LocalDate today) {
        if (date.isBefore(monthStart) || date.isAfter(today)) {
            throw new BusinessException("Solo se pueden eliminar ingresos del mes en curso");
        }
    }

    private static String employeeName(com.gym.management.model.Employee employee) {
        if (employee == null) {
            return "—";
        }
        return employee.getFirstName() + " " + employee.getLastName();
    }
}
