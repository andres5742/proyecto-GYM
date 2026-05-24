package com.gym.management.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.dto.InventoryMissingLineDto;
import com.gym.management.dto.SurplusInventoryResolutionResult;
import com.gym.management.exception.BusinessException;
import com.gym.management.model.CashShortfallKind;
import com.gym.management.model.CashShortfallStatus;
import com.gym.management.model.Employee;
import com.gym.management.model.EmployeeCashShortfall;
import com.gym.management.repository.EmployeeCashShortfallRepository;
import com.gym.management.util.MoneyUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cruza sobrante de efectivo (y crédito por productos sobrantes en conteo) con descuadres de inventario
 * pendientes del mismo empleado y día. Puede liquidar total o parcialmente; el resto sigue en Descuadres.
 */
@Service
@RequiredArgsConstructor
public class InventorySurplusResolutionService {

    private static final TypeReference<List<InventoryMissingLineDto>> INVENTORY_LINES_TYPE =
            new TypeReference<>() {};

    private final EmployeeCashShortfallRepository shortfallRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Optional<String> tryResolveHandoverSurplus(
            Employee employee, LocalDate shiftDate, BigDecimal surplusAmount, Employee settledBy) {
        SurplusInventoryResolutionResult result =
                applyCreditToPendingInventory(employee, shiftDate, surplusAmount, settledBy);
        return result.userMessage();
    }

    @Transactional
    public SurplusInventoryResolutionResult applyCreditToPendingInventory(
            Employee employee, LocalDate shiftDate, BigDecimal creditAmount, Employee settledBy) {
        BigDecimal credit = MoneyUtil.roundPesos(creditAmount);
        if (credit.compareTo(BigDecimal.ZERO) <= 0) {
            return emptyResult(credit);
        }
        List<EmployeeCashShortfall> pending = shortfallRepository
                .findPendingInventoryByEmployeeAndDate(
                        employee.getId(), shiftDate, CashShortfallStatus.PENDING, CashShortfallKind.INVENTORY)
                .stream()
                .sorted(Comparator.comparing(EmployeeCashShortfall::getCreatedAt))
                .toList();
        if (pending.isEmpty()) {
            return new SurplusInventoryResolutionResult(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    credit,
                    false,
                    Optional.empty());
        }

        BigDecimal remainingCredit = credit;
        BigDecimal appliedTotal = BigDecimal.ZERO;
        for (EmployeeCashShortfall record : pending) {
            if (remainingCredit.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal owed = MoneyUtil.roundPesos(record.getShortfallAmount());
            if (owed.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (remainingCredit.compareTo(owed) >= 0) {
                settleRecordFully(record, settledBy, owed, true);
                remainingCredit = remainingCredit.subtract(owed);
                appliedTotal = appliedTotal.add(owed);
            } else {
                partialSettleRecord(record, settledBy, remainingCredit);
                appliedTotal = appliedTotal.add(remainingCredit);
                remainingCredit = BigDecimal.ZERO;
                break;
            }
        }

        BigDecimal remainingDebt = MoneyUtil.roundPesos(shortfallRepository
                .findPendingInventoryByEmployeeAndDate(
                        employee.getId(), shiftDate, CashShortfallStatus.PENDING, CashShortfallKind.INVENTORY)
                .stream()
                .map(EmployeeCashShortfall::getShortfallAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        Optional<String> message = Optional.empty();
        if (appliedTotal.compareTo(BigDecimal.ZERO) > 0) {
            StringBuilder msg = new StringBuilder();
            msg.append("Se aplicaron ")
                    .append(MoneyUtil.formatPesos(appliedTotal))
                    .append(" del sobrante a faltante de productos");
            if (remainingDebt.compareTo(BigDecimal.ZERO) > 0) {
                msg.append("; queda debiendo ")
                        .append(MoneyUtil.formatPesos(remainingDebt))
                        .append(" por inventario (Descuadres de caja)");
            } else {
                msg.append(". Faltante de productos cubierto con el sobrante: no queda deuda en Descuadres");
            }
            if (remainingCredit.compareTo(BigDecimal.ZERO) > 0) {
                msg.append(". Sobrante en efectivo no aplicado a inventario: ")
                        .append(MoneyUtil.formatPesos(remainingCredit));
            }
            message = Optional.of(msg.toString());
        }

        return new SurplusInventoryResolutionResult(
                appliedTotal,
                remainingDebt,
                remainingCredit,
                remainingDebt.compareTo(BigDecimal.ZERO) <= 0,
                message);
    }

    private SurplusInventoryResolutionResult emptyResult(BigDecimal credit) {
        return new SurplusInventoryResolutionResult(
                BigDecimal.ZERO, BigDecimal.ZERO, credit, false, Optional.empty());
    }

    private void settleRecordFully(
            EmployeeCashShortfall record, Employee settledBy, BigDecimal amount, boolean fullPayment) {
        Instant now = Instant.now();
        record.setStatus(CashShortfallStatus.SETTLED);
        record.setSettledAt(now);
        record.setSettledBy(settledBy);
        String note =
                fullPayment
                        ? "Liquidado con sobrante/crédito en entrega de turno ("
                                + MoneyUtil.formatPesos(amount)
                                + ")."
                        : "Abono parcial en entrega de turno (" + MoneyUtil.formatPesos(amount) + ").";
        appendNote(record, note);
        shortfallRepository.save(record);
    }

    private void partialSettleRecord(EmployeeCashShortfall record, Employee settledBy, BigDecimal payment) {
        BigDecimal owed = MoneyUtil.roundPesos(record.getShortfallAmount());
        BigDecimal paid = MoneyUtil.roundPesos(payment);
        if (paid.compareTo(owed) >= 0) {
            settleRecordFully(record, settledBy, owed, true);
            return;
        }
        BigDecimal remaining = owed.subtract(paid);
        List<InventoryMissingLineDto> lines = parseInventoryJson(record.getInventoryMissingJson());
        if (lines.isEmpty()) {
            record.setShortfallAmount(remaining);
            record.setExpectedAmount(remaining);
            record.setDeclaredAmount(BigDecimal.ZERO);
            appendNote(
                    record,
                    "Abono parcial en entrega: "
                            + MoneyUtil.formatPesos(paid)
                            + "; queda "
                            + MoneyUtil.formatPesos(remaining));
            shortfallRepository.save(record);
            return;
        }

        BigDecimal ratio = paid.divide(owed, 8, RoundingMode.HALF_UP);
        List<InventoryMissingLineDto> remainingLines = new ArrayList<>();
        for (InventoryMissingLineDto line : lines) {
            int missing = line.missingQuantity();
            if (missing <= 0) {
                continue;
            }
            int paidUnits = BigDecimal.valueOf(missing)
                    .multiply(ratio)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
            if (paidUnits > missing) {
                paidUnits = missing;
            }
            int remainUnits = missing - paidUnits;
            if (remainUnits > 0) {
                BigDecimal lineRemain = MoneyUtil.roundPesos(
                        line.unitPrice().multiply(BigDecimal.valueOf(remainUnits)));
                remainingLines.add(new InventoryMissingLineDto(
                        line.productId(),
                        line.productName(),
                        line.category(),
                        line.expectedQuantity(),
                        line.countedQuantity(),
                        remainUnits,
                        line.unitPrice(),
                        lineRemain));
            }
        }
        record.setShortfallAmount(remaining);
        record.setExpectedAmount(remaining);
        record.setDeclaredAmount(BigDecimal.ZERO);
        try {
            record.setInventoryMissingJson(
                    remainingLines.isEmpty() ? null : objectMapper.writeValueAsString(remainingLines));
        } catch (Exception e) {
            throw new BusinessException("No se pudo actualizar el detalle de inventario faltante");
        }
        appendNote(
                record,
                "Abono parcial en entrega: "
                        + MoneyUtil.formatPesos(paid)
                        + "; queda "
                        + MoneyUtil.formatPesos(remaining)
                        + " por productos.");
        shortfallRepository.save(record);
    }

    private void appendNote(EmployeeCashShortfall record, String note) {
        String existing = record.getNotes();
        record.setNotes(existing != null && !existing.isBlank() ? existing + " · " + note : note);
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
