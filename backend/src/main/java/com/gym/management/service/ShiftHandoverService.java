package com.gym.management.service;

import com.gym.management.dto.BillingHandoverCashBreakdown;
import com.gym.management.dto.HandoverDeliveredProductLine;
import com.gym.management.dto.HandoverInventorySnapshot;
import com.gym.management.dto.ProductInventoryCountItem;
import com.gym.management.dto.ShiftDetailResponse;
import com.gym.management.dto.ShiftHandoverComparisonResponse;
import com.gym.management.dto.ShiftHandoverExpenseRequest;
import com.gym.management.dto.ShiftHandoverPriorPaymentRequest;
import com.gym.management.dto.ShiftHandoverRequest;
import com.gym.management.dto.ShiftHandoverResponse;
import com.gym.management.dto.SalesSummaryResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.ShiftHandoverMapper;
import com.gym.management.model.PaymentMethod;
import com.gym.management.model.ShiftHandover;
import com.gym.management.model.ShiftHandoverExpense;
import com.gym.management.model.ShiftHandoverPriorPayment;
import com.gym.management.model.ShiftStatus;
import com.gym.management.model.Product;
import com.gym.management.model.WorkShift;
import com.gym.management.model.EmployeeCashShortfall;
import com.gym.management.repository.EmployeeCashShortfallRepository;
import com.gym.management.repository.ProductRepository;
import com.gym.management.repository.SaleRepository;
import com.gym.management.repository.ShiftHandoverRepository;
import com.gym.management.repository.WorkShiftRepository;
import com.gym.management.util.HandoverInventorySnapshotJson;
import com.gym.management.security.AuthenticatedUser;
import com.gym.management.security.SecurityUtils;
import com.gym.management.util.CashCountUtil;
import com.gym.management.util.MoneyUtil;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShiftHandoverService {

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private static final java.util.Set<com.gym.management.model.CashShortfallKind> CASH_ONLY_SHORTFALL_KINDS =
            java.util.EnumSet.of(
                    com.gym.management.model.CashShortfallKind.CASH_REGISTER,
                    com.gym.management.model.CashShortfallKind.CASH_SHIFT_OPEN);

    private final ShiftHandoverRepository handoverRepository;
    private final WorkShiftService workShiftService;
    private final SaleService saleService;
    private final BillingCashRegisterService billingCashRegisterService;
    private final WorkShiftRepository workShiftRepository;
    private final SaleRepository saleRepository;
    private final CashShortfallService cashShortfallService;
    private final EmployeeCashShortfallRepository shortfallRepository;
    private final ProductCreditService productCreditService;
    private final InventorySurplusResolutionService inventorySurplusResolutionService;
    private final ShiftInventoryService shiftInventoryService;
    private final ProductRepository productRepository;

    private record ExpectedCashTotals(
            BigDecimal billingCashInDrawer,
            BigDecimal billingCashBase,
            BigDecimal billingOtherIncomesCash,
            BigDecimal previousShiftSalesCash,
            BigDecimal previousShiftShortfallsDeducted,
            BigDecimal previousShiftCreditPaymentsCash,
            String previousShiftName,
            BigDecimal handoverShiftSalesCash,
            BigDecimal creditPaymentsCash,
            BigDecimal total,
            BigDecimal lastHandoverCashTotal,
            BigDecimal cashSinceLastHandover) {}

    @Transactional(readOnly = true)
    public List<ShiftHandoverResponse> findAll() {
        List<ShiftHandover> list;
        if (SecurityUtils.isSuperAdmin()) {
            list = handoverRepository.findAllByOrderBySubmittedAtDesc();
        } else {
            list = handoverRepository.findByWorkShift_ShiftDateOrderBySubmittedAtDesc(today());
        }
        return list.stream()
                .map(h -> toSummaryResponse(h, java.util.Optional.empty(), Optional.empty(), java.util.Optional.empty()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ShiftHandoverResponse findById(Long id) {
        ShiftHandover handover = getHandoverWithDetails(id);
        ensureCanView(handover);
        return toFullResponse(handover);
    }

    @Transactional(readOnly = true)
    public ShiftHandoverResponse previewForShift(Long workShiftId) {
        WorkShift shift = workShiftService.getShift(workShiftId);
        ensureCanManageShift(shift);
        if (handoverRepository.existsByWorkShiftId(workShiftId)) {
            return toFullResponse(getHandoverWithDetails(
                    handoverRepository.findByWorkShiftId(workShiftId).orElseThrow().getId()));
        }
        return buildPreviewResponse(shift);
    }

    @Transactional
    public void delete(Long id) {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException("Solo el super administrador puede eliminar entregas de turno");
        }
        ShiftHandover handover = handoverRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega de turno no encontrada: " + id));
        // El descuadre de caja referencia esta entrega; hay que quitarlo antes del DELETE.
        shortfallRepository.findByShiftHandoverId(id).ifPresent(shortfallRepository::delete);
        billingCashRegisterService.deleteHandoverCashSurplusIncome(id);
        billingCashRegisterService.deleteHandoverCashShortfallExpense(id);
        handoverRepository.delete(handover);
    }

    @Transactional
    public ShiftHandoverResponse submit(ShiftHandoverRequest request) {
        WorkShift shift = workShiftService.getShift(request.workShiftId());
        ensureCanManageShift(shift);
        if (handoverRepository.existsByWorkShiftId(shift.getId())) {
            throw new BusinessException("Este turno ya tiene una entrega registrada");
        }

        ShiftHandover handover = ShiftHandover.builder()
                .workShift(shift)
                .employee(shift.getEmployee())
                .bill2000(request.bill2000())
                .bill5000(request.bill5000())
                .bill10000(request.bill10000())
                .bill20000(request.bill20000())
                .bill50000(request.bill50000())
                .bill100000(request.bill100000())
                .coin1000(request.coin1000())
                .coin500(request.coin500())
                .coin200(request.coin200())
                .coin100(request.coin100())
                .coin50(request.coin50())
                .auxAmount(BigDecimal.ZERO)
                .nequiAmount(BigDecimal.ZERO)
                .bankAmount(BigDecimal.ZERO)
                .notes(request.notes())
                .submittedAt(Instant.now())
                .build();

        applyExpenses(handover, request.expenses());
        applyPriorPayments(handover, request.priorPayments());

        ShiftHandover saved = handoverRepository.save(handover);

        if (shift.getStatus() == ShiftStatus.OPEN) {
            workShiftService.close(shift.getId());
        }

        saved.setInventoryDeliveredJson(buildInventoryDeliveredJson(request.inventoryCounts()));
        saved = handoverRepository.save(saved);
        com.gym.management.dto.HandoverInventoryResult inventoryResult =
                shiftInventoryService.processAtHandover(shift, request.inventoryCounts(), saved);
        shortfallRepository.flush();

        ShiftDetailResponse detail = saleService.getShiftDetail(shift.getId());
        ExpectedCashTotals expected = computeExpectedCash(shift.getId(), detail.summary());
        BigDecimal declared = CashCountUtil.totalCash(saved);

        BigDecimal cashSurplus = declared.compareTo(expected.total()) > 0
                ? MoneyUtil.roundPesos(declared.subtract(expected.total()))
                : BigDecimal.ZERO;
        BigDecimal inventoryCredit = MoneyUtil.roundPesos(
                cashSurplus.add(inventoryResult.productSurplusCredit()));
        BigDecimal pendingInventoryDebt = shiftInventoryService.sumPendingInventoryShortfallForEmployee(
                shift.getShiftDate(), saved.getEmployee().getId());
        boolean hasInventoryDebt = pendingInventoryDebt.compareTo(BigDecimal.ZERO) > 0;

        com.gym.management.dto.SurplusInventoryResolutionResult inventoryResolution = null;
        Optional<String> surplusNote = Optional.empty();
        // Solo cruza sobrante con faltante de productos; el efectivo sobrante no usado queda en caja física.
        if (hasInventoryDebt && inventoryCredit.compareTo(BigDecimal.ZERO) > 0) {
            Long inventoryShortfallId = inventoryResult.newInventoryShortfall() != null
                    ? inventoryResult.newInventoryShortfall().id()
                    : null;
            inventoryResolution = inventorySurplusResolutionService.applyCreditToPendingInventory(
                    saved.getEmployee(),
                    shift.getShiftDate(),
                    inventoryCredit,
                    saved.getEmployee(),
                    inventoryShortfallId);
            surplusNote = inventoryResolution.userMessage();
        }

        java.util.Optional<com.gym.management.dto.CashShortfallResponse> shortfall =
                cashShortfallService.registerFromHandover(saved, expected.total(), declared);
        if (shortfall.isPresent()) {
            billingCashRegisterService.registerHandoverCashShortfallExpense(
                    saved, shortfall.get().shortfallAmount());
        }

        // Regla de negocio: el sobrante de efectivo SIEMPRE entra completo a caja (otros ingresos).
        // En paralelo, ese sobrante puede cruzar deuda de inventario para evitar cobro total/parcial al responsable.
        BigDecimal cashSurplusForBilling = cashSurplus;
        Optional<com.gym.management.dto.BillingCashRegisterOtherIncomeResponse> surplusBilling =
                billingCashRegisterService.registerHandoverCashSurplusAmount(saved, cashSurplusForBilling);

        billingCashRegisterService.recordLastHandoverPhysicalCash(
                shift.getShiftDate(), declared, saved.getSubmittedAt());

        return toSummaryResponse(saved, shortfall, surplusNote, surplusBilling);
    }

    /**
     * Backfill automático: crea en Facturación los sobrantes de entregas antiguas que no quedaron registrados.
     */
    @Transactional
    public int syncMissingHandoverCashSurplusInBilling() {
        int created = 0;
        List<ShiftHandover> handovers = handoverRepository.findAllByOrderBySubmittedAtDesc();
        for (ShiftHandover handover : handovers) {
            if (handover.getId() == null) {
                continue;
            }
            if (billingCashRegisterService.findHandoverCashSurplusIncome(handover.getId()).isPresent()) {
                continue;
            }
            ShiftDetailResponse detail = saleService.getShiftDetail(handover.getWorkShift().getId());
            ExpectedCashTotals expected = computeExpectedCash(handover.getWorkShift().getId(), detail.summary());
            BigDecimal declared = CashCountUtil.totalCash(handover);
            BigDecimal cashSurplus = declared.compareTo(expected.total()) > 0
                    ? MoneyUtil.roundPesos(declared.subtract(expected.total()))
                    : BigDecimal.ZERO;
            if (cashSurplus.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (billingCashRegisterService.registerHandoverCashSurplusAmount(handover, cashSurplus).isPresent()) {
                created++;
            }
        }
        return created;
    }

    private void applyExpenses(ShiftHandover handover, List<ShiftHandoverExpenseRequest> expenses) {
        if (expenses == null) {
            return;
        }
        for (ShiftHandoverExpenseRequest item : expenses) {
            if (item == null || item.description() == null || item.description().isBlank()) {
                continue;
            }
            handover.getExpenses()
                    .add(ShiftHandoverExpense.builder()
                            .handover(handover)
                            .description(item.description().trim())
                            .amount(item.amount())
                            .build());
        }
    }

    private void applyPriorPayments(ShiftHandover handover, List<ShiftHandoverPriorPaymentRequest> payments) {
        if (payments == null) {
            return;
        }
        for (ShiftHandoverPriorPaymentRequest item : payments) {
            if (item == null || item.description() == null || item.description().isBlank()) {
                continue;
            }
            if (item.paymentMethod() == PaymentMethod.PENDING
                    || item.paymentMethod() == PaymentMethod.AUX) {
                throw new BusinessException(
                        "En cobros de deudas anteriores use efectivo, Nequi o Bancolombia");
            }
            handover.getPriorPayments()
                    .add(ShiftHandoverPriorPayment.builder()
                            .handover(handover)
                            .description(item.description().trim())
                            .amount(item.amount())
                            .paymentMethod(item.paymentMethod())
                            .notes(item.notes())
                            .build());
        }
    }

    private ShiftHandoverResponse toSummaryResponse(
            ShiftHandover handover,
            java.util.Optional<com.gym.management.dto.CashShortfallResponse> shortfall,
            Optional<String> surplusResolutionNote,
            java.util.Optional<com.gym.management.dto.BillingCashRegisterOtherIncomeResponse> surplusBilling) {
        BigDecimal expensesTotal = sumExpenses(handover);
        BigDecimal priorTotal = sumPriorPayments(handover);
        ShiftDetailResponse detail = saleService.getShiftDetail(handover.getWorkShift().getId());
        ExpectedCashTotals expected = computeExpectedCash(handover.getWorkShift().getId(), detail.summary());
        java.math.BigDecimal registeredShortfall = shortfall
                .map(com.gym.management.dto.CashShortfallResponse::shortfallAmount)
                .orElse(null);
        Long shortfallId = shortfall.map(com.gym.management.dto.CashShortfallResponse::id).orElse(null);
        if (registeredShortfall == null && handover.getId() != null) {
            java.util.Optional<com.gym.management.dto.CashShortfallResponse> existing =
                    cashShortfallService.findByHandoverId(handover.getId());
            registeredShortfall =
                    existing.map(com.gym.management.dto.CashShortfallResponse::shortfallAmount).orElse(null);
            shortfallId = existing.map(com.gym.management.dto.CashShortfallResponse::id).orElse(null);
        }
        InventoryPreviewContext inventory = inventoryPreviewContext(handover.getWorkShift());
        HandoverInventorySnapshot deliveredSnapshot = parseDeliveredInventory(handover.getInventoryDeliveredJson());
        java.util.Optional<com.gym.management.dto.BillingCashRegisterOtherIncomeResponse> surplusIncome =
                surplusBilling.isPresent()
                        ? surplusBilling
                        : billingCashRegisterService.findHandoverCashSurplusIncome(handover.getId());
        BigDecimal registeredSurplus = surplusIncome
                .map(com.gym.management.dto.BillingCashRegisterOtherIncomeResponse::amount)
                .orElse(null);
        Long surplusOtherIncomeId =
                surplusIncome.map(com.gym.management.dto.BillingCashRegisterOtherIncomeResponse::id).orElse(null);
        return ShiftHandoverMapper.toResponse(
                handover,
                expensesTotal,
                priorTotal,
                detail,
                expected.billingCashInDrawer(),
                expected.billingCashBase(),
                expected.billingOtherIncomesCash(),
                expected.previousShiftSalesCash(),
                expected.previousShiftShortfallsDeducted(),
                expected.previousShiftName(),
                expected.handoverShiftSalesCash(),
                expected.previousShiftCreditPaymentsCash(),
                expected.creditPaymentsCash(),
                expected.total(),
                expected.lastHandoverCashTotal(),
                expected.cashSinceLastHandover(),
                inventory.products(),
                deliveredSnapshot.unitsTotal(),
                countProductKinds(deliveredSnapshot.lines()),
                deliveredSnapshot.lines(),
                inventory.pendingTotal(),
                buildComparisons(handover, detail.summary(), expected),
                registeredShortfall,
                shortfallId,
                surplusResolutionNote.isPresent(),
                surplusResolutionNote.orElse(null),
                registeredSurplus != null && registeredSurplus.compareTo(BigDecimal.ZERO) > 0,
                registeredSurplus,
                surplusOtherIncomeId);
    }

    private record InventoryPreviewContext(
            java.util.List<com.gym.management.dto.ProductInventoryLineResponse> products,
            BigDecimal pendingTotal) {}

    private InventoryPreviewContext inventoryPreviewContext(WorkShift shift) {
        java.util.List<com.gym.management.dto.ProductInventoryLineResponse> products =
                shiftInventoryService.listActiveProductLines();
        BigDecimal pending = shiftInventoryService.sumPendingInventoryShortfallForEmployee(
                shift.getShiftDate(), shift.getEmployee().getId());
        return new InventoryPreviewContext(products, pending);
    }

    private ShiftHandoverResponse toFullResponse(ShiftHandover handover) {
        ShiftHandover loaded = getHandoverWithDetails(handover.getId());
        return toSummaryResponse(loaded, java.util.Optional.empty(), Optional.empty(), java.util.Optional.empty());
    }

    private ShiftHandoverResponse buildPreviewResponse(WorkShift shift) {
        ShiftDetailResponse detail = saleService.getShiftDetail(shift.getId());
        ShiftHandover empty = ShiftHandover.builder()
                .workShift(shift)
                .employee(shift.getEmployee())
                .submittedAt(Instant.now())
                .build();
        ExpectedCashTotals expected = computeExpectedCash(shift.getId(), detail.summary());
        InventoryPreviewContext inventory = inventoryPreviewContext(shift);
        return ShiftHandoverMapper.toResponse(
                empty,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                detail,
                expected.billingCashInDrawer(),
                expected.billingCashBase(),
                expected.billingOtherIncomesCash(),
                expected.previousShiftSalesCash(),
                expected.previousShiftShortfallsDeducted(),
                expected.previousShiftName(),
                expected.handoverShiftSalesCash(),
                expected.previousShiftCreditPaymentsCash(),
                expected.creditPaymentsCash(),
                expected.total(),
                expected.lastHandoverCashTotal(),
                expected.cashSinceLastHandover(),
                inventory.products(),
                0,
                0,
                List.of(),
                inventory.pendingTotal(),
                buildComparisons(empty, detail.summary(), expected),
                null,
                null,
                false,
                null,
                false,
                null,
                null);
    }

    private static int countProductKinds(java.util.List<HandoverDeliveredProductLine> lines) {
        if (lines == null) {
            return 0;
        }
        return (int) lines.stream().filter(l -> l.stockRemaining() > 0).count();
    }

    private HandoverInventorySnapshot parseDeliveredInventory(String json) {
        return HandoverInventorySnapshotJson.read(json);
    }

    private String buildInventoryDeliveredJson(List<ProductInventoryCountItem> counts) {
        if (counts == null || counts.isEmpty()) {
            return null;
        }
        Map<Long, Product> productsById = new HashMap<>();
        for (Product product : productRepository.findByActiveTrueOrderByNameAsc()) {
            productsById.put(product.getId(), product);
        }
        List<HandoverDeliveredProductLine> lines = new ArrayList<>();
        int unitsTotal = 0;
        for (ProductInventoryCountItem item : counts) {
            if (item.productId() == null || item.countedQuantity() == null) {
                continue;
            }
            Product product = productsById.get(item.productId());
            if (product == null) {
                continue;
            }
            int expected = product.getQuantity();
            int stockRemaining = item.countedQuantity();
            unitsTotal += stockRemaining;
            lines.add(new HandoverDeliveredProductLine(
                    product.getId(), product.getName(), product.getCategory(), expected, stockRemaining));
        }
        lines.sort(java.util.Comparator.comparing(HandoverDeliveredProductLine::productName, String.CASE_INSENSITIVE_ORDER));
        return HandoverInventorySnapshotJson.write(new HandoverInventorySnapshot(unitsTotal, lines));
    }

    private LocalDate today() {
        return LocalDate.now(GYM_ZONE);
    }

    private ExpectedCashTotals computeExpectedCash(Long handoverShiftId, SalesSummaryResponse sales) {
        WorkShift shift = workShiftService.getShift(handoverShiftId);
        com.gym.management.dto.CashInDrawerTotals drawer =
                billingCashRegisterService.cashInDrawerTotalsForDate(shift.getShiftDate());
        BillingHandoverCashBreakdown billingBreakdown =
                billingCashRegisterService.billingHandoverCashBreakdownForDate(shift.getShiftDate());
        BigDecimal handoverShiftCash =
                sales.amountByPaymentMethod().getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        BigDecimal creditCash = productCreditService.sumCashPaymentsForShift(handoverShiftId);
        PreviousShiftCash previous = resolvePreviousShiftCash(handoverShiftId);
        // Entrega de turno: esperado = base de Facturación + ventas/cobros en efectivo de turnos.
        BigDecimal total = billingBreakdown.total()
                .add(previous.netSalesCash())
                .add(previous.creditPaymentsCash())
                .add(handoverShiftCash)
                .add(creditCash);
        total = MoneyUtil.roundPesos(total);
        return new ExpectedCashTotals(
                billingBreakdown.total(),
                billingBreakdown.cashBase(),
                billingBreakdown.otherIncomesCash(),
                previous.netSalesCash(),
                previous.shortfallsDeducted(),
                previous.creditPaymentsCash(),
                previous.name(),
                handoverShiftCash,
                creditCash,
                total,
                drawer.lastHandoverCashTotal(),
                drawer.cashSinceLastHandover());
    }

    private record PreviousShiftCash(
            BigDecimal netSalesCash, BigDecimal shortfallsDeducted, BigDecimal creditPaymentsCash, String name) {}

    private PreviousShiftCash resolvePreviousShiftCash(Long handoverShiftId) {
        WorkShift current = workShiftService.getShift(handoverShiftId);
        List<WorkShift> previousShifts = workShiftRepository.findByShiftDateAndStatusAndOpenedAtBeforeOrderByOpenedAtAsc(
                current.getShiftDate(), ShiftStatus.CLOSED, current.getOpenedAt());
        if (previousShifts.isEmpty()) {
            return new PreviousShiftCash(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
        }
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal deductedTotal = BigDecimal.ZERO;
        BigDecimal creditCashTotal = BigDecimal.ZERO;
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < previousShifts.size(); i++) {
            WorkShift shift = previousShifts.get(i);
            BigDecimal cash = saleRepository.sumTotalByPaymentMethodAndShift(PaymentMethod.CASH, shift.getId());
            if (cash == null) {
                cash = BigDecimal.ZERO;
            }
            BigDecimal shortfall = shortfallRepository.sumShortfallAmountByWorkShiftIdAndKinds(
                    shift.getId(), CASH_ONLY_SHORTFALL_KINDS);
            deductedTotal = deductedTotal.add(shortfall);
            BigDecimal net = cash.subtract(shortfall);
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                netTotal = netTotal.add(net);
            }
            BigDecimal shiftCreditCash = productCreditService.sumCashPaymentsForShift(shift.getId());
            creditCashTotal = creditCashTotal.add(shiftCreditCash);
            if (i > 0) {
                names.append(", ");
            }
            names.append(shift.getName());
        }
        return new PreviousShiftCash(netTotal, deductedTotal, creditCashTotal, names.toString());
    }

    private List<ShiftHandoverComparisonResponse> buildComparisons(
            ShiftHandover handover, SalesSummaryResponse sales, ExpectedCashTotals expected) {
        BigDecimal cashCounted = CashCountUtil.totalCash(handover);
        return List.of(comparison(
                "Dinero contado (billetes + monedas)",
                cashCounted,
                expected.total()));
    }

    private ShiftHandoverComparisonResponse comparison(String label, BigDecimal declared, BigDecimal expected) {
        return new ShiftHandoverComparisonResponse(label, declared, expected, declared.subtract(expected));
    }

    private BigDecimal sumExpenses(ShiftHandover handover) {
        return handover.getExpenses().stream()
                .map(ShiftHandoverExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumPriorPayments(ShiftHandover handover) {
        return handover.getPriorPayments().stream()
                .map(ShiftHandoverPriorPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ShiftHandover getHandoverWithDetails(Long id) {
        ShiftHandover handover = handoverRepository
                .findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega de turno no encontrada: " + id));
        handover.getExpenses().size();
        handover.getPriorPayments().size();
        return handover;
    }

    private void ensureCanView(ShiftHandover handover) {
        if (SecurityUtils.isSuperAdmin()) {
            return;
        }
        if (!handover.getWorkShift().getShiftDate().equals(today())) {
            throw new BusinessException("Solo el super administrador puede ver entregas de otros días");
        }
    }

    private void ensureCanManageShift(WorkShift shift) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        AuthenticatedUser user = requireUser();
        Long employeeId = user.employeeId();
        if (employeeId == null) {
            throw new BusinessException(
                    "Tu usuario no está vinculado a un empleado. Pide al administrador que lo asocie.");
        }
        if (shift.getEmployee() == null) {
            throw new BusinessException("El turno no tiene vendedor asignado.");
        }
        if (shift.getEmployee().getId().equals(employeeId)) {
            return;
        }
        throw new BusinessException("Solo quien abrió el turno puede registrar la entrega.");
    }

    private AuthenticatedUser requireUser() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user == null) {
            throw new BusinessException("Sesión no válida");
        }
        return user;
    }
}
