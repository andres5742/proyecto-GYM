package com.gym.management.service;

import com.gym.management.admin.AdminDataCleanupScope;
import com.gym.management.dto.AdminDataCleanupResponse;
import com.gym.management.dto.AdminDataCleanupScopeResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.security.SecurityUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDataCleanupService {

    public static final String CONFIRM_PHRASE = "LIMPIAR";

    private final JdbcTemplate jdbc;

    public AdminDataCleanupService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AdminDataCleanupScopeResponse> listScopes() {
        List<AdminDataCleanupScopeResponse> list = new ArrayList<>();
        for (AdminDataCleanupScope scope : AdminDataCleanupScope.values()) {
            list.add(new AdminDataCleanupScopeResponse(
                    scope.name(), scope.getLabel(), scope.getDescription()));
        }
        return list;
    }

    @Transactional
    public AdminDataCleanupResponse cleanup(AdminDataCleanupScope scope, String confirmPhrase) {
        requireSuperAdmin();
        if (!CONFIRM_PHRASE.equals(confirmPhrase != null ? confirmPhrase.trim() : "")) {
            throw new BusinessException("Escribe exactamente «" + CONFIRM_PHRASE + "» para confirmar");
        }
        Map<String, Long> details = new LinkedHashMap<>();
        switch (scope) {
            case BILLING -> cleanBilling(details);
            case SALES_AND_SHIFTS -> cleanSalesAndShifts(details);
            case FIADO -> cleanFiado(details);
            case ACCESS_LOGS -> cleanAccessLogs(details);
            case MEMBER_BIOMETRICS -> cleanMemberBiometrics(details);
            case MEMBERS -> cleanMembers(details);
            case FEEDBACK -> cleanFeedback(details);
            case TRAINER_RATINGS -> cleanTrainerRatings(details);
            case WORK_ATTENDANCE -> cleanWorkAttendance(details);
            case WALL_POSTS -> cleanWallPosts(details);
            case ALL_OPERATIONAL -> cleanAllOperational(details);
            default -> throw new BusinessException("Ámbito de limpieza no válido");
        }
        long total = details.values().stream().mapToLong(Long::longValue).sum();
        return new AdminDataCleanupResponse(scope.name(), total, details);
    }

    private void cleanAllOperational(Map<String, Long> details) {
        cleanBilling(details);
        cleanFiado(details);
        cleanSalesAndShifts(details);
        cleanAccessLogs(details);
        cleanFeedback(details);
        cleanTrainerRatings(details);
        cleanWorkAttendance(details);
        cleanWallPosts(details);
        cleanMemberBiometrics(details);
        cleanMembers(details);
    }

    private void cleanBilling(Map<String, Long> details) {
        deleteTable(details, "billing_cash_register_other_incomes");
        deleteTable(details, "billing_cash_register_expenses");
        deleteTable(details, "billing_payments");
        deleteTable(details, "billing_cash_registers");
    }

    private void cleanSalesAndShifts(Map<String, Long> details) {
        cleanFiado(details);
        deleteWhere(details, "billing_payments", "sale_id IS NOT NULL OR work_shift_id IS NOT NULL");
        deleteTable(details, "employee_cash_shortfalls");
        deleteTable(details, "shift_handover_expenses");
        deleteTable(details, "shift_handover_prior_payments");
        deleteTable(details, "shift_handovers");
        deleteTable(details, "sales");
        deleteTable(details, "work_shifts");
    }

    private void cleanFiado(Map<String, Long> details) {
        deleteTable(details, "product_credit_payments");
        deleteTable(details, "product_credits");
    }

    private void cleanAccessLogs(Map<String, Long> details) {
        deleteTable(details, "access_logs");
    }

    private void cleanMemberBiometrics(Map<String, Long> details) {
        deleteTable(details, "member_biometric_credentials");
        deleteTable(details, "member_face_embeddings");
        deleteTable(details, "member_fingerprints");
    }

    private void cleanMembers(Map<String, Long> details) {
        cleanFiado(details);
        deleteWhere(details, "billing_payments", "member_id IS NOT NULL");
        deleteTable(details, "member_progress_photos");
        deleteTable(details, "member_progress_entries");
        cleanMemberBiometrics(details);
        cleanAccessLogs(details);
        deleteTable(details, "members");
    }

    private void cleanFeedback(Map<String, Long> details) {
        deleteTable(details, "feedback_message_images");
        deleteTable(details, "feedback_messages");
    }

    private void cleanTrainerRatings(Map<String, Long> details) {
        deleteTable(details, "trainer_ratings");
    }

    private void cleanWorkAttendance(Map<String, Long> details) {
        deleteTable(details, "work_attendance");
    }

    private void cleanWallPosts(Map<String, Long> details) {
        deleteTable(details, "wall_post_images");
        deleteTable(details, "wall_posts");
    }

    private void deleteTable(Map<String, Long> details, String table) {
        long deleted = deleteAll(table);
        if (deleted > 0) {
            details.merge(table, deleted, Long::sum);
        }
    }

    private void deleteWhere(Map<String, Long> details, String table, String whereClause) {
        long count = countWhere(table, whereClause);
        if (count == 0) {
            return;
        }
        jdbc.update("DELETE FROM " + table + " WHERE " + whereClause);
        details.merge(table + " (" + whereClause + ")", count, Long::sum);
    }

    private long deleteAll(String table) {
        long count = countAll(table);
        if (count == 0) {
            return 0;
        }
        jdbc.update("DELETE FROM " + table);
        return count;
    }

    private long countAll(String table) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return count != null ? count : 0;
    }

    private long countWhere(String table, String whereClause) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + whereClause, Long.class);
        return count != null ? count : 0;
    }

    private static void requireSuperAdmin() {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException("Solo el super administrador puede limpiar datos");
        }
    }
}
