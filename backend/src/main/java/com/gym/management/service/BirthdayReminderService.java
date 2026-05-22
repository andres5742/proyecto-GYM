package com.gym.management.service;

import com.gym.management.dto.UpcomingBirthdayResponse;
import com.gym.management.model.BirthdayPersonType;
import com.gym.management.model.Employee;
import com.gym.management.model.Member;
import com.gym.management.repository.EmployeeRepository;
import com.gym.management.repository.MemberRepository;
import com.gym.management.util.BirthdayUtils;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BirthdayReminderService {

    public static final int DEFAULT_WITHIN_DAYS = 7;

    private static final ZoneId GYM_ZONE = ZoneId.of("America/Bogota");

    private final MemberRepository memberRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<UpcomingBirthdayResponse> findUpcoming(int withinDays) {
        int window = withinDays > 0 ? withinDays : DEFAULT_WITHIN_DAYS;
        LocalDate today = LocalDate.now(GYM_ZONE);
        List<UpcomingBirthdayResponse> results = new ArrayList<>();

        for (Member member : memberRepository.findByBirthDateNotNull()) {
            addIfWithinWindow(
                    results,
                    BirthdayPersonType.MEMBER,
                    member.getId(),
                    member.getFirstName() + " " + member.getLastName(),
                    member.getBirthDate(),
                    today,
                    window);
        }

        for (Employee employee : employeeRepository.findByBirthDateNotNullAndActiveTrue()) {
            if (!EmployeeVisibility.visibleInTeamDirectory(employee)) {
                continue;
            }
            addIfWithinWindow(
                    results,
                    BirthdayPersonType.EMPLOYEE,
                    employee.getId(),
                    employee.getFirstName() + " " + employee.getLastName(),
                    employee.getBirthDate(),
                    today,
                    window);
        }

        results.sort(
                Comparator.comparingInt(UpcomingBirthdayResponse::daysUntil)
                        .thenComparing(UpcomingBirthdayResponse::celebrationDate)
                        .thenComparing(UpcomingBirthdayResponse::fullName, String.CASE_INSENSITIVE_ORDER));
        return results;
    }

    private static void addIfWithinWindow(
            List<UpcomingBirthdayResponse> results,
            BirthdayPersonType type,
            Long id,
            String fullName,
            LocalDate birthDate,
            LocalDate today,
            int withinDays) {
        if (birthDate == null) {
            return;
        }
        int daysUntil = BirthdayUtils.daysUntilNextBirthday(birthDate, today);
        if (daysUntil < 0 || daysUntil > withinDays) {
            return;
        }
        LocalDate celebration = BirthdayUtils.nextCelebrationDate(birthDate, today);
        results.add(new UpcomingBirthdayResponse(
                type,
                type.label(),
                id,
                fullName,
                celebration,
                daysUntil,
                BirthdayUtils.ageTurningOn(birthDate, celebration)));
    }
}
