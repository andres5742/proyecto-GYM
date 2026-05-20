package com.gym.management.service;

import com.gym.management.dto.ProductCreditPayAllRequest;
import com.gym.management.dto.ProductCreditPayAllResponse;
import com.gym.management.dto.ProductCreditPaymentRequest;
import com.gym.management.dto.ProductCreditPaymentResponse;
import com.gym.management.dto.ProductCreditRequest;
import com.gym.management.dto.ProductCreditResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.ProductCreditMapper;
import com.gym.management.model.Employee;
import com.gym.management.model.Member;
import com.gym.management.model.PaymentMethod;
import com.gym.management.model.Product;
import com.gym.management.model.ProductCredit;
import com.gym.management.model.ProductCreditPayment;
import com.gym.management.model.ProductCreditStatus;
import com.gym.management.model.Sale;
import com.gym.management.model.ShiftStatus;
import com.gym.management.model.WorkShift;
import com.gym.management.repository.MemberRepository;
import com.gym.management.repository.ProductCreditPaymentRepository;
import com.gym.management.repository.ProductCreditRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductCreditService {

    private final ProductCreditRepository creditRepository;
    private final ProductCreditPaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final ProductService productService;
    private final WorkShiftService workShiftService;

    @Transactional(readOnly = true)
    public List<ProductCreditResponse> findAll(ProductCreditStatus status) {
        List<ProductCredit> list = status != null
                ? creditRepository.findByStatusWithRelations(status)
                : creditRepository.findAllWithRelations();
        return list.stream().map(c -> ProductCreditMapper.toResponse(c, false)).toList();
    }

    @Transactional(readOnly = true)
    public ProductCreditResponse findById(Long id) {
        ProductCredit credit = creditRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fiado no encontrado"));
        return ProductCreditMapper.toResponse(credit, true);
    }

    @Transactional
    public ProductCreditResponse create(ProductCreditRequest request) {
        WorkShift shift = resolveOpenShift(request.workShiftId());
        Employee employee = shift.getEmployee();
        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new BusinessException("El vendedor del turno no está activo");
        }

        Member member = memberRepository
                .findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado"));
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

        ProductCredit credit = ProductCredit.builder()
                .member(member)
                .product(product)
                .employee(employee)
                .workShift(shift)
                .quantity(request.quantity())
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .balance(totalAmount)
                .status(ProductCreditStatus.OPEN)
                .creditedAt(LocalDateTime.now())
                .notes(request.notes())
                .build();

        return ProductCreditMapper.toResponse(creditRepository.save(credit), false);
    }

    @Transactional
    public ProductCreditPaymentResponse registerPayment(Long creditId, ProductCreditPaymentRequest request) {
        ProductCredit credit = creditRepository
                .findDetailedById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiado no encontrado"));

        if (credit.getStatus() != ProductCreditStatus.OPEN) {
            throw new BusinessException("Este fiado ya está cerrado");
        }
        if (request.paymentMethod() == PaymentMethod.PENDING || request.paymentMethod() == PaymentMethod.AUX) {
            throw new BusinessException("Use efectivo, Nequi o Bancolombia para registrar el pago");
        }
        if (request.amount().compareTo(credit.getBalance()) > 0) {
            throw new BusinessException(
                    "El pago supera el saldo pendiente (" + credit.getBalance() + ")");
        }

        WorkShift shift = resolveOpenShift(request.workShiftId());
        Employee employee = shift.getEmployee();

        ProductCreditPayment payment = ProductCreditPayment.builder()
                .credit(credit)
                .employee(employee)
                .workShift(shift)
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .paidAt(LocalDateTime.now())
                .notes(request.notes())
                .build();
        paymentRepository.save(payment);

        BigDecimal newBalance = credit.getBalance().subtract(request.amount());
        credit.setBalance(newBalance);
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            credit.setStatus(ProductCreditStatus.PAID);
            credit.setBalance(BigDecimal.ZERO);
        }
        creditRepository.save(credit);

        return ProductCreditMapper.toPaymentResponse(payment);
    }

    /**
     * Registra fiado vinculado a una venta PENDING (el stock ya se descontó en la venta).
     */
    @Transactional
    public void createFromPendingSale(Sale sale, Long memberId) {
        if (sale.getId() == null) {
            throw new BusinessException("La venta debe guardarse antes de registrar el fiado");
        }
        if (memberId == null) {
            throw new BusinessException(
                    "Indique el afiliado al que se fiaron las unidades en pendiente/deuda");
        }
        if (creditRepository.existsByOriginSaleId(sale.getId())) {
            return;
        }
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado"));

        ProductCredit credit = ProductCredit.builder()
                .member(member)
                .product(sale.getProduct())
                .employee(sale.getEmployee())
                .workShift(sale.getWorkShift())
                .originSale(sale)
                .quantity(sale.getQuantity())
                .unitPrice(sale.getUnitPrice())
                .totalAmount(sale.getTotalAmount())
                .balance(sale.getTotalAmount())
                .status(ProductCreditStatus.OPEN)
                .creditedAt(sale.getSaleDate())
                .notes("Registrado desde ventas (pendiente/deuda)")
                .build();
        creditRepository.save(credit);
    }

    /** Liquida todas las líneas OPEN del afiliado (pago completo de cada producto fiado). */
    @Transactional
    public ProductCreditPayAllResponse payAllForMember(Long memberId, ProductCreditPayAllRequest request) {
        memberRepository
                .findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado"));
        List<ProductCredit> openCredits =
                creditRepository.findByMemberIdAndStatusOrderByCreditedAtAsc(memberId, ProductCreditStatus.OPEN);
        if (openCredits.isEmpty()) {
            throw new BusinessException("Este afiliado no tiene deuda pendiente");
        }
        List<ProductCreditPaymentResponse> payments = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ProductCredit credit : openCredits) {
            BigDecimal amount = credit.getBalance();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            ProductCreditPaymentRequest line = new ProductCreditPaymentRequest(
                    amount, request.paymentMethod(), request.workShiftId(), request.notes());
            payments.add(registerPayment(credit.getId(), line));
            total = total.add(amount);
        }
        if (payments.isEmpty()) {
            throw new BusinessException("No hay saldo pendiente por cobrar");
        }
        return new ProductCreditPayAllResponse(payments.size(), total, payments);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumCashPaymentsForShift(Long workShiftId) {
        if (workShiftId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = paymentRepository.sumAmountByShiftAndMethod(workShiftId, PaymentMethod.CASH);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private WorkShift resolveOpenShift(Long workShiftId) {
        WorkShift shift;
        if (workShiftId != null) {
            shift = workShiftService.getShift(workShiftId);
        } else {
            shift = workShiftService.getOpenShiftRequired();
        }
        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new BusinessException("El turno está cerrado. Abra un turno para registrar fiado o cobros.");
        }
        return shift;
    }
}
