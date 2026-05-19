package com.gym.management.service;

import com.gym.management.dto.MemberImportResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.model.Gender;
import com.gym.management.model.Member;
import com.gym.management.model.MembershipPlan;
import com.gym.management.model.MembershipStatus;
import com.gym.management.repository.MemberRepository;
import com.gym.management.repository.MembershipPlanRepository;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MemberImportService {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("uuuu-M-d"),
            DateTimeFormatter.ofPattern("d-M-uuuu"));

    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberImportResponse importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Selecciona un archivo Excel");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new BusinessException("El archivo debe ser Excel (.xlsx o .xls)");
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        List<MembershipPlan> allPlans = planRepository.findAll();
        Map<String, MembershipPlan> plansByName = loadPlansByName(allPlans);
        DataFormatter formatter = new DataFormatter();

        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BusinessException("El archivo no tiene hojas");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BusinessException("La primera fila debe tener los encabezados de columna");
            }

            Map<String, Integer> columns = mapColumns(headerRow, formatter);
            if (!columns.containsKey("id")) {
                throw new BusinessException("Falta la columna obligatoria: id");
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isRowEmpty(row, formatter)) {
                    continue;
                }
                int excelRow = rowIndex + 1;
                try {
                    String documentId = cellText(row, columns.get("id"), formatter);
                    if (documentId.isBlank()) {
                        skipped++;
                        continue;
                    }

                    String fullName = cellText(row, columns.get("nombre"), formatter);
                    if (fullName.isBlank()) {
                        errors.add("Fila " + excelRow + ": falta el nombre");
                        continue;
                    }

                    String[] nameParts = splitName(fullName);
                    String email = resolveEmail(row, columns, formatter, documentId);
                    String phone = resolvePhone(row, columns, formatter);
                    String landline = cellText(row, columns.get("tel"), formatter);
                    String memberCode = cellText(row, columns.get("codigo"), formatter);
                    String eps = cellText(row, columns.get("eps"), formatter);
                    int currentYear = LocalDate.now().getYear();
                    Optional<LocalDate> membershipEndOpt = tryParseDate(row, columns.get("fechav"), formatter);
                    if (membershipEndOpt.isEmpty()) {
                        skipped++;
                        continue;
                    }
                    LocalDate membershipEnd = membershipEndOpt.get();
                    if (membershipEnd.getYear() != currentYear) {
                        skipped++;
                        continue;
                    }

                    LocalDate birthDate = tryParseDate(row, columns.get("fechanac"), formatter).orElse(null);
                    LocalDate membershipStart = tryParseDate(row, columns.get("fechac"), formatter).orElse(null);
                    MembershipStatus status = MemberMembershipRules.resolveStatus(
                            cellText(row, columns.get("estado"), formatter), membershipEnd);
                    MembershipPlan plan =
                            resolvePlan(cellText(row, columns.get("servicio"), formatter), allPlans, plansByName);

                    Optional<Member> existing = memberRepository.findByDocumentId(documentId);
                    if (existing.isPresent()) {
                        Member member = existing.get();
                        if (memberRepository.existsByEmailAndIdNot(email, member.getId())) {
                            errors.add("Fila " + excelRow + ": el correo ya está en uso por otro afiliado");
                            continue;
                        }
                        applyRow(
                                member,
                                nameParts,
                                email,
                                phone,
                                landline,
                                documentId,
                                memberCode,
                                eps,
                                null,
                                birthDate,
                                status,
                                plan,
                                membershipStart,
                                membershipEnd);
                        memberRepository.save(member);
                        updated++;
                    } else {
                        if (memberRepository.existsByEmail(email)) {
                            errors.add("Fila " + excelRow + ": el documento ya está registrado");
                            continue;
                        }
                        Member member = new Member();
                        applyRow(
                                member,
                                nameParts,
                                email,
                                phone,
                                landline,
                                documentId,
                                memberCode,
                                eps,
                                null,
                                birthDate,
                                status,
                                plan,
                                membershipStart,
                                membershipEnd);
                        MemberService.applyDefaultPortalPassword(member, passwordEncoder);
                        memberRepository.save(member);
                        created++;
                    }
                } catch (Exception ex) {
                    errors.add("Fila " + excelRow + ": " + ex.getMessage());
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException("No se pudo leer el archivo Excel");
        } catch (Exception ex) {
            throw new BusinessException("Archivo Excel inválido o corrupto");
        }

        memberService.syncExpiredMemberships();
        return new MemberImportResponse(created, updated, skipped, errors);
    }

    private Map<String, MembershipPlan> loadPlansByName(List<MembershipPlan> plans) {
        Map<String, MembershipPlan> map = new HashMap<>();
        for (MembershipPlan plan : plans) {
            String key = normalizeHeader(plan.getName());
            map.put(key, plan);
            for (String alias : planAliases(key)) {
                map.putIfAbsent(alias, plan);
            }
        }
        return map;
    }

    private List<String> planAliases(String normalizedPlanName) {
        List<String> aliases = new ArrayList<>();
        if (normalizedPlanName.contains("mensual")) {
            aliases.add("mensualidad");
        }
        if (normalizedPlanName.contains("quincen") || normalizedPlanName.contains("qincen")) {
            aliases.add("quincena");
            aliases.add("qincena");
        }
        if (normalizedPlanName.contains("entren")) {
            aliases.add("entrenos");
            aliases.add("entreno");
        }
        if (normalizedPlanName.contains("trimest")) {
            aliases.add("trimestral");
        }
        if (normalizedPlanName.contains("anual")) {
            aliases.add("anualidad");
        }
        return aliases;
    }

    private Map<String, Integer> mapColumns(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : headerRow) {
            String key = normalizeHeader(formatter.formatCellValue(cell));
            if (!key.isBlank()) {
                columns.put(key, cell.getColumnIndex());
            }
        }
        return columns;
    }

    private void applyRow(
            Member member,
            String[] nameParts,
            String email,
            String phone,
            String landline,
            String documentId,
            String memberCode,
            String eps,
            Gender gender,
            LocalDate birthDate,
            MembershipStatus status,
            MembershipPlan plan,
            LocalDate membershipStart,
            LocalDate membershipEnd) {
        member.setFirstName(nameParts[0]);
        member.setLastName(nameParts[1]);
        member.setEmail(email);
        member.setPhone(blankToNull(phone));
        member.setLandlinePhone(blankToNull(landline));
        member.setDocumentId(documentId);
        member.setMemberCode(blankToNull(memberCode));
        member.setEps(blankToNull(eps));
        member.setGender(gender);
        member.setBirthDate(birthDate);
        member.setStatus(status);

        if (plan != null) {
            member.setPlan(plan);
            LocalDate start = membershipStart != null ? membershipStart : LocalDate.now();
            member.setMembershipStart(start);
            member.setMembershipEnd(membershipEnd != null ? membershipEnd : start.plusDays(plan.getDurationDays()));
        } else {
            member.setPlan(null);
            member.setMembershipStart(membershipStart);
            member.setMembershipEnd(membershipEnd);
        }
        MemberMembershipRules.applyExpirationStatus(member);
    }

    private String resolveEmail(Row row, Map<String, Integer> columns, DataFormatter formatter, String documentId) {
        String mail = cellText(row, columns.get("mail"), formatter);
        if (!mail.isBlank()) {
            return mail.toLowerCase(Locale.ROOT);
        }
        return documentId.replaceAll("\\s+", "") + "@sin-correo.importado";
    }

    private String resolvePhone(Row row, Map<String, Integer> columns, DataFormatter formatter) {
        String cel = cellText(row, columns.get("cel"), formatter);
        if (!cel.isBlank()) {
            return cel;
        }
        return cellText(row, columns.get("tel"), formatter);
    }

    private MembershipPlan resolvePlan(
            String serviceName, List<MembershipPlan> allPlans, Map<String, MembershipPlan> plansByName) {
        if (serviceName.isBlank()) {
            return null;
        }

        String normalizedService = normalizeHeader(serviceName);
        MembershipPlan exact = plansByName.get(normalizedService);
        if (exact != null) {
            return exact;
        }

        if (normalizedService.contains("mensual")) {
            return findPlanByKeyword(allPlans, "mensual")
                    .orElseThrow(() -> new BusinessException(
                            "No hay un plan mensual en Planes. Crea uno con nombre tipo «Mensual» o «Mensualidad»."));
        }
        if (normalizedService.contains("quincen") || normalizedService.contains("qincen")) {
            return findPlanByKeyword(allPlans, "quincen").orElse(null);
        }
        if (normalizedService.contains("entren")) {
            return findPlanByKeyword(allPlans, "entren").orElse(null);
        }
        if (normalizedService.contains("trimest")) {
            return findPlanByKeyword(allPlans, "trimest").orElse(null);
        }
        if (normalizedService.contains("anual")) {
            return findPlanByKeyword(allPlans, "anual").orElse(null);
        }

        return null;
    }

    private Optional<MembershipPlan> findPlanByKeyword(List<MembershipPlan> plans, String keyword) {
        return plans.stream()
                .filter(plan -> normalizeHeader(plan.getName()).contains(keyword))
                .findFirst();
    }

    private String[] splitName(String fullName) {
        String trimmed = fullName.trim().replaceAll("\\s+", " ");
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new String[] {trimmed, "."};
        }
        return new String[] {trimmed.substring(0, space), trimmed.substring(space + 1).trim()};
    }

    private Optional<LocalDate> tryParseDate(Row row, Integer columnIndex, DataFormatter formatter) {
        if (columnIndex == null) {
            return Optional.empty();
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return Optional.empty();
        }
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return Optional.of(cell.getLocalDateTimeCellValue().toLocalDate());
            }
            double numeric = cell.getNumericCellValue();
            if (DateUtil.isValidExcelDate(numeric)) {
                return Optional.of(DateUtil.getLocalDateTime(numeric).toLocalDate());
            }
            return Optional.empty();
        }
        String text = formatter.formatCellValue(cell).trim();
        if (text.isBlank() || !text.matches(".*\\d.*")) {
            return Optional.empty();
        }
        for (DateTimeFormatter pattern : DATE_FORMATS) {
            try {
                return Optional.of(LocalDate.parse(text, pattern));
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return Optional.empty();
    }

    private String cellText(Row row, Integer columnIndex, DataFormatter formatter) {
        if (columnIndex == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        return NON_ALNUM.matcher(normalized).replaceAll("");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
