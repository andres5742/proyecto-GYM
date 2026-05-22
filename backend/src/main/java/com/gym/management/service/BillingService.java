package com.gym.management.service;

import com.gym.management.dto.BillingDailySummaryResponse;
import com.gym.management.dto.BillingMonthlySummaryResponse;
import com.gym.management.dto.BillingPaymentResponse;
import com.gym.management.dto.DayWorkoutRegisterRequest;
import com.gym.management.dto.DayWorkoutRegisterResponse;
import com.gym.management.dto.AccessOnboardingData;
import com.gym.management.dto.AccessOnboardingKind;
import com.gym.management.dto.BiometricEnrollRequest;
import com.gym.management.dto.FaceWebcamEnrollRequest;
import com.gym.management.dto.MembershipObligationResponse;
import com.gym.management.dto.MembershipOnboardingRequest;
import com.gym.management.dto.MembershipOnboardingResponse;
import com.gym.management.dto.MembershipPaymentOutcomeResponse;
import com.gym.management.dto.MembershipPaymentRequest;
import com.gym.management.dto.MemberResponse;
import com.gym.management.mapper.MemberMapper;
import com.gym.management.model.BillingCashRegister;
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
import com.gym.management.repository.BillingCashRegisterExpenseRepository;
import com.gym.management.repository.BillingPaymentRepository;
import com.gym.management.repository.MemberRepository;
import com.gym.management.repository.MembershipPlanRepository;
import com.gym.management.repository.SaleRepository;
import com.gym.management.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
    private final BillingCashRegisterExpenseRepository expenseRepository;
    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;
    private final SaleRepository saleRepository;
    private final EmployeeService employeeService;
    private final TurnstileGatewayService turnstileGatewayService;
    private final AccessLogRepository accessLogRepository;
    private final BillingCashRegisterService billingCashRegisterService;
    private final MembershipObligationService membershipObligationService;
    private final MemberService memberService;
    private final AccessControlService accessControlService;
    private final FaceWebcamService faceWebcamService;

    @Value("${app.billing.day-workout-plan-name:Entreno día}")
    private String dayWorkoutPlanName;

    @Value("${app.billing.sports-dance-plan-name:Bailes deportivos}")
    private String sportsDancePlanName;

    @Transactional(readOnly = true)
    public List<BillingPaymentResponse> listByDate(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now(GYM_ZONE);
        return billingPaymentRepository.findByPaymentDateWithMember(target).stream()
                .map(BillingPaymentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BillingMonthlySummaryResponse monthlySummary(int year, int month) {
        YearMonth period = YearMonth.of(year, month);
        LocalDate start = period.atDay(1);
        LocalDate end = period.atEndOfMonth();

        Map<PaymentMethod, BigDecimal> byMethod = sumBetweenDates(start, end, null);
        Map<PaymentMethod, BigDecimal> dayByMethod =
                sumBetweenDates(start, end, BillingPaymentType.DAY_WORKOUT);
        Map<PaymentMethod, BigDecimal> sportsDanceByMethod =
                sumBetweenDates(start, end, BillingPaymentType.SPORTS_DANCE);
        Map<PaymentMethod, BigDecimal> membershipByMethod =
                sumBetweenDates(start, end, BillingPaymentType.MEMBERSHIP);

        long totalPayments = billingPaymentRepository.countBetweenDatesExcludingPending(start, end);
        Map<PaymentMethod, BigDecimal> expensesByMethod =
                sumExpensesBetweenDates(start, end);
        BigDecimal totalExpenses = sumMap(expensesByMethod);
        long expenseCount = expenseRepository.countBetweenDates(start, end);

        return new BillingMonthlySummaryResponse(
                year,
                month,
                totalPayments,
                sumMap(byMethod),
                totalExpenses,
                expenseCount,
                byMethod,
                dayByMethod,
                sportsDanceByMethod,
                membershipByMethod,
                expensesByMethod);
    }

    @Transactional(readOnly = true)
    public BillingDailySummaryResponse dailySummary(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now(GYM_ZONE);
        Map<PaymentMethod, BigDecimal> dayByMethod = sumByMethod(target, BillingPaymentType.DAY_WORKOUT);
        Map<PaymentMethod, BigDecimal> sportsDanceByMethod = sumByMethod(target, BillingPaymentType.SPORTS_DANCE);
        Map<PaymentMethod, BigDecimal> membershipByMethod = sumByMethod(target, BillingPaymentType.MEMBERSHIP);

        long dayCount = countByTypeOnDate(target, BillingPaymentType.DAY_WORKOUT);
        long sportsDanceCount = countByTypeOnDate(target, BillingPaymentType.SPORTS_DANCE);
        long membershipCount = countByTypeOnDate(target, BillingPaymentType.MEMBERSHIP);

        BigDecimal dayTotal = sumMap(dayByMethod);
        BigDecimal sportsDanceTotal = sumMap(sportsDanceByMethod);
        BigDecimal membershipTotal = sumMap(membershipByMethod);
        Map<PaymentMethod, BigDecimal> incomeByMethod =
                BillingPaymentMethodTotals.mergeAll(dayByMethod, sportsDanceByMethod, membershipByMethod);
        Map<PaymentMethod, BigDecimal> expensesByMethod = sumExpensesBetweenDates(target, target);
        BigDecimal expensesTotal = sumMap(expensesByMethod);
        long expenseCount = expenseRepository.countBetweenDates(target, target);

        return new BillingDailySummaryResponse(
                target,
                dayCount,
                dayTotal,
                dayByMethod,
                sportsDanceCount,
                sportsDanceTotal,
                sportsDanceByMethod,
                membershipCount,
                membershipTotal,
                membershipByMethod,
                incomeByMethod,
                expenseCount,
                expensesTotal,
                expensesByMethod,
                dayTotal.add(sportsDanceTotal).add(membershipTotal));
    }

    private static final String GUEST_LABEL = "Invitado";

    @Transactional
    public DayWorkoutRegisterResponse registerDayWorkoutAndOpenGate(DayWorkoutRegisterRequest request) {
        return registerDayPassAndOpenGate(
                request,
                BillingPaymentType.DAY_WORKOUT,
                dayWorkoutPlanName,
                "Este afiliado ya tiene un entreno del día registrado hoy.",
                "ENTRENO-BILL-");
    }

    @Transactional
    public DayWorkoutRegisterResponse registerSportsDanceAndOpenGate(DayWorkoutRegisterRequest request) {
        return registerDayPassAndOpenGate(
                request,
                BillingPaymentType.SPORTS_DANCE,
                sportsDancePlanName,
                "Este afiliado ya tiene un baile deportivo activado hoy.",
                "BAILES-BILL-");
    }

    private DayWorkoutRegisterResponse registerDayPassAndOpenGate(
            DayWorkoutRegisterRequest request,
            BillingPaymentType paymentType,
            String configuredPlanName,
            String duplicateMemberMessage,
            String accessLogDevicePrefix) {
        Member member = null;
        if (request.memberId() != null) {
            member = memberRepository
                    .findById(request.memberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + request.memberId()));
            LocalDate today = LocalDate.now(GYM_ZONE);
            if (billingPaymentRepository.existsByMemberIdAndPaymentTypeAndPaymentDate(
                    member.getId(), paymentType, today)) {
                throw new BusinessException(duplicateMemberMessage);
            }
        }

        String planName = configuredPlanName.trim();
        MembershipPlan dayPlan = planRepository
                .findByNameIgnoreCase(planName)
                .orElseThrow(() -> new BusinessException(
                        "Configure el plan «" + planName + "» en Planes (1 día de duración)."));

        PaymentMethod method = request.paymentMethod();
        BillingPaymentMethodRules.requireAllowed(method);
        BillingCashRegister cashRegister = billingCashRegisterService.getOpenRegisterRequired();
        LocalDate today = LocalDate.now(GYM_ZONE);
        Employee registeredBy = resolveRegisteringEmployee();

        String personLabel = member != null
                ? member.getFirstName() + " " + member.getLastName()
                : GUEST_LABEL;

        BigDecimal amount = dayPlan.getPrice();
        BillingPayment.BillingPaymentBuilder billingBuilder = BillingPayment.builder()
                .paymentType(paymentType)
                .billingCashRegister(cashRegister)
                .plan(dayPlan)
                .employee(registeredBy)
                .paymentMethod(method)
                .amount(amount)
                .paymentDate(today)
                .membershipStart(today)
                .membershipEnd(today)
                .notes(member != null ? "Pase diario afiliado · " + planName : "Pase diario invitado · " + planName);

        if (member != null) {
            billingBuilder.member(member);
        } else {
            billingBuilder.guestLabel(GUEST_LABEL);
        }
        BillingPayment billing = billingPaymentRepository.save(billingBuilder.build());

        String announcementSpeech = buildDayPassAnnouncementSpeech(paymentType, member);

        String deviceUserId = accessLogDevicePrefix + billing.getId();
        Long gatePersonId = member != null ? member.getId() : billing.getId();
        boolean gateOpened = turnstileGatewayService.openGate(personLabel, gatePersonId);

        AccessLog.AccessLogBuilder logBuilder = AccessLog.builder()
                .fingerprintUserId(deviceUserId)
                .credentialType(BiometricCredentialType.FINGERPRINT)
                .result(gateOpened ? AccessResult.GRANTED : AccessResult.DENIED)
                .message(announcementSpeech)
                .gateOpened(gateOpened);
        if (member != null) {
            logBuilder.member(member);
        }
        accessLogRepository.save(logBuilder.build());

        String registeredLabel = paymentType == BillingPaymentType.SPORTS_DANCE
                ? "Baile deportivo activado"
                : "Entreno registrado";
        String message = gateOpened
                ? "Torniquete abierto · " + announcementSpeech
                : registeredLabel + " pero no se abrió el torniquete · " + announcementSpeech;

        return new DayWorkoutRegisterResponse(
                gateOpened, message, announcementSpeech, BillingPaymentMapper.toResponse(billing));
    }

    private static String buildDayPassAnnouncementSpeech(BillingPaymentType paymentType, Member member) {
        String firstName =
                member != null && member.getFirstName() != null
                        ? member.getFirstName().trim()
                        : "";
        if (paymentType == BillingPaymentType.SPORTS_DANCE) {
            return firstName.isEmpty()
                    ? "Baile deportivo activado."
                    : "Baile deportivo activado, " + firstName + ".";
        }
        return firstName.isEmpty() ? "Entreno registrado." : "Entreno registrado, " + firstName + ".";
    }

    @Transactional(readOnly = true)
    public MembershipObligationResponse findOpenMembershipObligation(Long memberId) {
        return membershipObligationService
                .findOpenForMember(memberId)
                .orElse(null);
    }

    @Transactional
    public MembershipPaymentOutcomeResponse registerMembershipPayment(MembershipPaymentRequest request) {
        Member member = memberRepository
                .findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + request.memberId()));

        MembershipPlan plan = planRepository
                .findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado: " + request.planId()));

        requireMembershipBillablePlan(plan);

        BillingCashRegister cashRegister = billingCashRegisterService.getOpenRegisterRequired();

        return membershipObligationService.registerPayment(
                member,
                plan,
                request.monthsPaid(),
                request.paymentMethod(),
                request.amount(),
                cashRegister,
                resolveRegisteringEmployee(),
                request.obligationId());
    }

    @Transactional
    public MembershipOnboardingResponse registerMembershipOnboarding(MembershipOnboardingRequest request) {
        if (request.memberId() == null && request.newMember() == null) {
            throw new BusinessException("Selecciona un afiliado existente o registra uno nuevo");
        }
        if (request.memberId() != null && request.newMember() != null) {
            throw new BusinessException("Indica solo un afiliado: existente o nuevo, no ambos");
        }

        MembershipPlan plan = planRepository
                .findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado: " + request.planId()));

        requireMembershipBillablePlan(plan);

        BillingPaymentMethodRules.requireAllowed(request.paymentMethod());
        BillingCashRegister cashRegister = billingCashRegisterService.getOpenRegisterRequired();

        Member member;
        if (request.memberId() != null) {
            member = memberRepository
                    .findById(request.memberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + request.memberId()));
        } else {
            member = memberService.createForBilling(request.newMember());
        }

        MembershipPaymentOutcomeResponse outcome = membershipObligationService.registerPayment(
                member,
                plan,
                request.monthsPaid(),
                request.paymentMethod(),
                request.amount(),
                cashRegister,
                resolveRegisteringEmployee(),
                request.obligationId());

        AccessEnrollmentResult accessResult = registerAccessIfPresent(member.getId(), request.access());

        return new MembershipOnboardingResponse(
                MemberMapper.toResponse(member),
                outcome.payment(),
                outcome.obligation(),
                outcome.membershipActivated(),
                outcome.balanceRemaining(),
                outcome.message(),
                accessResult.registered(),
                accessResult.message());
    }

    private record AccessEnrollmentResult(boolean registered, String message) {}

    private void requireMembershipBillablePlan(MembershipPlan plan) {
        if (isDayPassPlan(plan)) {
            throw new BusinessException(
                    "Entreno del día y bailes deportivos se registran con F2 y F3, no como membresía.");
        }
    }

    private boolean isDayPassPlan(MembershipPlan plan) {
        String name = plan.getName() != null ? plan.getName().trim() : "";
        return name.equalsIgnoreCase(dayWorkoutPlanName.trim())
                || name.equalsIgnoreCase(sportsDancePlanName.trim());
    }

    private AccessEnrollmentResult registerAccessIfPresent(Long memberId, AccessOnboardingData access) {
        if (access == null || access.kind() == null) {
            return new AccessEnrollmentResult(false, "Sin registro de acceso en este paso");
        }
        if (access.kind() == AccessOnboardingKind.FINGERPRINT) {
            if (access.deviceUserId() == null || access.deviceUserId().isBlank()) {
                throw new BusinessException("Indica el ID de huella del lector");
            }
            accessControlService.enroll(new BiometricEnrollRequest(
                    memberId,
                    null,
                    access.deviceUserId().trim(),
                    BiometricCredentialType.FINGERPRINT,
                    access.deviceLabel()));
            return new AccessEnrollmentResult(true, "Huella vinculada correctamente");
        }
        if (access.kind() == AccessOnboardingKind.CARD) {
            if (access.deviceUserId() == null || access.deviceUserId().isBlank()) {
                throw new BusinessException("Indica el número de tarjeta del lector");
            }
            accessControlService.enroll(new BiometricEnrollRequest(
                    memberId,
                    null,
                    access.deviceUserId().trim(),
                    BiometricCredentialType.CARD,
                    access.deviceLabel()));
            return new AccessEnrollmentResult(true, "Tarjeta vinculada correctamente");
        }
        if (access.kind() == AccessOnboardingKind.FACE) {
            if (access.faceDescriptor() == null || access.faceDescriptor().size() != 128) {
                throw new BusinessException("Captura el rostro antes de guardar");
            }
            faceWebcamService.enroll(new FaceWebcamEnrollRequest(memberId, null, access.faceDescriptor()));
            return new AccessEnrollmentResult(true, "Rostro registrado correctamente");
        }
        return new AccessEnrollmentResult(false, "Tipo de acceso no reconocido");
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
            membershipObligationService.revertPayment(payment);
            if (payment.getMembershipObligation() == null) {
                tryRevertMembershipOnDelete(payment);
            }
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

    private long countByTypeOnDate(LocalDate date, BillingPaymentType type) {
        return billingPaymentRepository.findByPaymentDateWithMember(date).stream()
                .filter(p -> p.getPaymentType() == type)
                .filter(p -> BillingPaymentMethodRules.isBillable(p.getPaymentMethod()))
                .count();
    }

    private Map<PaymentMethod, BigDecimal> sumByMethod(LocalDate date, BillingPaymentType type) {
        return fillMethodMap(billingPaymentRepository.sumByPaymentMethodAndDateAndType(date, type));
    }

    private Map<PaymentMethod, BigDecimal> sumBetweenDates(
            LocalDate start, LocalDate end, BillingPaymentType type) {
        List<Object[]> rows = type == null
                ? billingPaymentRepository.sumByPaymentMethodBetweenDates(start, end)
                : billingPaymentRepository.sumByPaymentMethodBetweenDatesAndType(start, end, type);
        return fillMethodMap(rows);
    }

    private Map<PaymentMethod, BigDecimal> sumExpensesBetweenDates(LocalDate start, LocalDate end) {
        return BillingPaymentMethodTotals.fromAmountRows(expenseRepository.sumByPaymentMethodBetweenDates(start, end));
    }

    private static Map<PaymentMethod, BigDecimal> fillMethodMap(List<Object[]> rows) {
        Map<PaymentMethod, BigDecimal> map = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            if (BillingPaymentMethodRules.isBillable(method)) {
                map.put(method, BigDecimal.ZERO);
            }
        }
        for (Object[] row : rows) {
            PaymentMethod method = (PaymentMethod) row[0];
            BigDecimal total = (BigDecimal) row[1];
            if (BillingPaymentMethodRules.isBillable(method)) {
                map.put(method, total);
            }
        }
        return map;
    }

    private static BigDecimal sumMap(Map<PaymentMethod, BigDecimal> map) {
        return map.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
