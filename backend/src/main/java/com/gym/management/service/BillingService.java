package com.gym.management.service;

import com.gym.management.dto.BillingDailySummaryResponse;
import com.gym.management.dto.BillingPaymentResponse;
import com.gym.management.dto.DayWorkoutRegisterRequest;
import com.gym.management.dto.DayWorkoutRegisterResponse;
import com.gym.management.dto.MembershipPaymentRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.BillingPaymentMapper;
import com.gym.management.model.AccessLog;
import com.gym.management.model.AccessResult;
import com.gym.management.model.BillingPayment;
import com.gym.management.model.BillingPaymentType;
import com.gym.management.model.BiometricCredentialType;
import com.gym.management.model.Employee;
import com.gym.management.model.Member;
import com.gym.management.model.MembershipPlan;
import com.gym.management.model.MembershipStatus;
import com.gym.management.model.PaymentMethod;
import com.gym.management.model.Sale;
import com.gym.management.model.Product;
import com.gym.management.repository.AccessLogRepository;
import com.gym.management.repository.BillingPaymentRepository;
import com.gym.management.repository.MemberRepository;
import com.gym.management.repository.MembershipPlanRepository;
import com.gym.management.repository.SaleRepository;
import com.gym.management.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private final BillingPaymentRepository billingPaymentRepository;
    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;
    private final SaleRepository saleRepository;
    private final EmployeeService employeeService;
    private final TurnstileGatewayService turnstileGatewayService;
    private final AccessLogRepository accessLogRepository;

    @Value("${app.billing.day-workout-plan-name:Entreno día}")
    private String dayWorkoutPlanName;

    @Transactional(readOnly = true)
    public List<BillingPaymentResponse> listByDate(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now(GYM_ZONE);
        return billingPaymentRepository.findByPaymentDateWithMember(target).stream()
                .map(BillingPaymentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BillingDailySummaryResponse dailySummary(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now(GYM_ZONE);
        Map<PaymentMethod, BigDecimal> dayByMethod = sumByMethod(target, BillingPaymentType.DAY_WORKOUT);
        Map<PaymentMethod, BigDecimal> membershipByMethod = sumByMethod(target, BillingPaymentType.MEMBERSHIP);

        long dayCount = billingPaymentRepository.findByPaymentDateWithMember(target).stream()
                .filter(p -> p.getPaymentType() == BillingPaymentType.DAY_WORKOUT)
                .count();
        long membershipCount = billingPaymentRepository.findByPaymentDateWithMember(target).stream()
                .filter(p -> p.getPaymentType() == BillingPaymentType.MEMBERSHIP)
                .count();

        BigDecimal dayTotal = sumMap(dayByMethod);
        BigDecimal membershipTotal = sumMap(membershipByMethod);

        return new BillingDailySummaryResponse(
                target,
                dayCount,
                dayTotal,
                dayByMethod,
                membershipCount,
                membershipTotal,
                membershipByMethod,
                dayTotal.add(membershipTotal));
    }

    private static final String GUEST_LABEL = "Invitado";

    @Transactional
    public DayWorkoutRegisterResponse registerDayWorkoutAndOpenGate(DayWorkoutRegisterRequest request) {
        Member member = null;
        if (request.memberId() != null) {
            member = memberRepository
                    .findById(request.memberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + request.memberId()));
            LocalDate today = LocalDate.now(GYM_ZONE);
            if (billingPaymentRepository.existsByMemberIdAndPaymentTypeAndPaymentDate(
                    member.getId(), BillingPaymentType.DAY_WORKOUT, today)) {
                throw new BusinessException("Este afiliado ya tiene un entreno del día registrado hoy.");
            }
        }

        MembershipPlan dayPlan = planRepository
                .findByNameIgnoreCase(dayWorkoutPlanName.trim())
                .orElseThrow(() -> new BusinessException(
                        "Configure el plan «" + dayWorkoutPlanName + "» en Planes (1 día de duración)."));

        PaymentMethod method = request.paymentMethod();
        LocalDate today = LocalDate.now(GYM_ZONE);
        Employee registeredBy = resolveRegisteringEmployee();

        String personLabel = member != null
                ? member.getFirstName() + " " + member.getLastName()
                : GUEST_LABEL;

        BigDecimal amount = dayPlan.getPrice();
        BillingPayment.BillingPaymentBuilder billingBuilder = BillingPayment.builder()
                .paymentType(BillingPaymentType.DAY_WORKOUT)
                .plan(dayPlan)
                .employee(registeredBy)
                .paymentMethod(method)
                .amount(amount)
                .paymentDate(today)
                .membershipStart(today)
                .membershipEnd(today)
                .notes(member != null ? "Pase diario afiliado" : "Pase diario invitado");

        if (member != null) {
            billingBuilder.member(member);
        } else {
            billingBuilder.guestLabel(GUEST_LABEL);
        }
        BillingPayment billing = billingPaymentRepository.save(billingBuilder.build());

        String speechText = "Entreno registrado.";
        String deviceUserId = "ENTRENO-BILL-" + billing.getId();
        Long gatePersonId = member != null ? member.getId() : billing.getId();
        boolean gateOpened = turnstileGatewayService.openGate(personLabel, gatePersonId);

        AccessLog.AccessLogBuilder logBuilder = AccessLog.builder()
                .fingerprintUserId(deviceUserId)
                .credentialType(BiometricCredentialType.FINGERPRINT)
                .result(gateOpened ? AccessResult.GRANTED : AccessResult.DENIED)
                .message(speechText)
                .gateOpened(gateOpened);
        if (member != null) {
            logBuilder.member(member);
        }
        accessLogRepository.save(logBuilder.build());

        String message = gateOpened
                ? "Torniquete abierto · " + speechText
                : "Entreno registrado pero no se abrió el torniquete · " + speechText;

        return new DayWorkoutRegisterResponse(
                gateOpened, message, speechText, BillingPaymentMapper.toResponse(billing));
    }

    @Transactional
    public BillingPaymentResponse registerMembershipPayment(MembershipPaymentRequest request) {
        Member member = memberRepository
                .findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + request.memberId()));

        MembershipPlan plan = planRepository
                .findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado: " + request.planId()));

        if (plan.getDurationDays() <= 1) {
            throw new BusinessException("Para pase de un día use F2 (entreno del día) o el plan «" + dayWorkoutPlanName + "».");
        }

        LocalDate start = LocalDate.now(GYM_ZONE);
        LocalDate end = start.plusDays(plan.getDurationDays());
        member.setPlan(plan);
        member.setMembershipStart(start);
        member.setMembershipEnd(end);
        member.setStatus(MembershipStatus.ACTIVE);
        memberRepository.save(member);

        BillingPayment billing = billingPaymentRepository.save(BillingPayment.builder()
                .paymentType(BillingPaymentType.MEMBERSHIP)
                .member(member)
                .plan(plan)
                .employee(resolveRegisteringEmployee())
                .paymentMethod(request.paymentMethod())
                .amount(plan.getPrice())
                .paymentDate(start)
                .membershipStart(start)
                .membershipEnd(end)
                .notes("Membresía " + plan.getName())
                .build());

        return BillingPaymentMapper.toResponse(billing);
    }

    @Transactional
    public void deleteTodayPayment(Long id) {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException("Solo el super administrador puede eliminar movimientos de hoy");
        }
        BillingPayment payment = billingPaymentRepository
                .findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento no encontrado: " + id));

        LocalDate today = LocalDate.now(GYM_ZONE);
        if (!payment.getPaymentDate().equals(today)) {
            throw new BusinessException("Solo se pueden eliminar movimientos del día actual");
        }

        if (payment.getPaymentType() == BillingPaymentType.MEMBERSHIP) {
            tryRevertMembershipOnDelete(payment);
        } else if (payment.getSale() != null) {
            // Compatibilidad con registros antiguos vinculados a ventas
            Sale sale = payment.getSale();
            Product product = sale.getProduct();
            if (product != null) {
                product.setQuantity(product.getQuantity() + sale.getQuantity());
            }
            saleRepository.delete(sale);
        }

        billingPaymentRepository.delete(payment);
    }

    private Employee resolveRegisteringEmployee() {
        Long employeeId = SecurityUtils.currentEmployeeId();
        if (employeeId == null) {
            return null;
        }
        return employeeService.getEmployee(employeeId);
    }

    /**
     * Si la membresía del afiliado sigue siendo la de este pago, la revierte.
     * Si ya cambió (otro plan, renovación, etc.), solo se borra el movimiento de facturación.
     */
    private void tryRevertMembershipOnDelete(BillingPayment payment) {
        Member member = payment.getMember();
        if (member == null) {
            return;
        }
        if (!membershipMatchesPayment(member, payment)) {
            return;
        }
        member.setStatus(MembershipStatus.EXPIRED);
        if (payment.getMembershipStart() != null) {
            member.setMembershipEnd(payment.getMembershipStart().minusDays(1));
        }
        memberRepository.save(member);
    }

    private static boolean membershipMatchesPayment(Member member, BillingPayment payment) {
        Long memberPlanId = member.getPlan() != null ? member.getPlan().getId() : null;
        Long paymentPlanId = payment.getPlan() != null ? payment.getPlan().getId() : null;
        return Objects.equals(memberPlanId, paymentPlanId)
                && Objects.equals(member.getMembershipStart(), payment.getMembershipStart())
                && Objects.equals(member.getMembershipEnd(), payment.getMembershipEnd())
                && member.getStatus() == MembershipStatus.ACTIVE;
    }

    private Map<PaymentMethod, BigDecimal> sumByMethod(LocalDate date, BillingPaymentType type) {
        Map<PaymentMethod, BigDecimal> map = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            map.put(method, BigDecimal.ZERO);
        }
        for (Object[] row : billingPaymentRepository.sumByPaymentMethodAndDateAndType(date, type)) {
            PaymentMethod method = (PaymentMethod) row[0];
            BigDecimal total = (BigDecimal) row[1];
            map.put(method, total);
        }
        return map;
    }

    private static BigDecimal sumMap(Map<PaymentMethod, BigDecimal> map) {
        return map.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
