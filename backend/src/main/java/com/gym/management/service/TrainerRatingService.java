package com.gym.management.service;

import com.gym.management.dto.RatingParticipantAdminResponse;
import com.gym.management.dto.RatingParticipantUpdateRequest;
import com.gym.management.dto.TrainerRatingMonthlySummary;
import com.gym.management.dto.TrainerRatingParticipantResponse;
import com.gym.management.dto.TrainerRatingSubmitRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.Employee;
import com.gym.management.model.TrainerRating;
import com.gym.management.model.UserRole;
import com.gym.management.repository.EmployeeRepository;
import com.gym.management.repository.TrainerRatingRepository;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrainerRatingService {

    private static final ZoneId RATING_ZONE = ZoneId.of("America/Bogota");
    private static final java.util.Set<UserRole> RATING_PARTICIPANT_ROLES =
            java.util.Set.of(UserRole.TRAINER, UserRole.ADMIN, UserRole.SUPER_ADMIN);

    private final TrainerRatingRepository trainerRatingRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<TrainerRatingParticipantResponse> findPublicParticipants() {
        return employeeRepository
                .findByRatingEligibleTrueAndActiveTrueOrderByFirstNameAsc()
                .stream()
                .map(this::toParticipantResponse)
                .toList();
    }

    @Transactional
    public void submit(TrainerRatingSubmitRequest request) {
        Employee employee = employeeRepository
                .findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Entrenador no encontrado"));
        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new BusinessException("Este entrenador no está disponible para calificar");
        }
        if (!Boolean.TRUE.equals(employee.getRatingEligible())) {
            throw new BusinessException("Esta persona no participa en la calificación");
        }
        if (!canParticipateInRating(employee.getRole())) {
            throw new BusinessException("Solo se pueden calificar entrenadores y administradores");
        }

        String identification = normalizeIdentification(request.identificationNumber());
        YearMonth currentMonth = YearMonth.now(RATING_ZONE);
        int year = currentMonth.getYear();
        int month = currentMonth.getMonthValue();

        if (trainerRatingRepository.existsByEmployeeIdAndIdentificationNumberAndRatingYearAndRatingMonth(
                employee.getId(), identification, year, month)) {
            throw new BusinessException(
                    "Ya calificaste a este entrenador este mes. Solo puedes hacerlo una vez por mes.");
        }

        TrainerRating rating = TrainerRating.builder()
                .employee(employee)
                .score(request.score())
                .identificationNumber(identification)
                .ratingYear(year)
                .ratingMonth(month)
                .build();
        trainerRatingRepository.save(rating);
    }

    @Transactional(readOnly = true)
    public List<RatingParticipantAdminResponse> findAllForConfig() {
        return employeeRepository.findAllByOrderByFirstNameAsc().stream()
                .filter(e -> canParticipateInRating(e.getRole()))
                .map(this::toAdminParticipant)
                .toList();
    }

    @Transactional
    public RatingParticipantAdminResponse updateParticipant(Long id, RatingParticipantUpdateRequest request) {
        Employee employee = employeeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrenador no encontrado: " + id));
        if (!canParticipateInRating(employee.getRole())) {
            throw new BusinessException("Solo entrenadores y administradores pueden participar en la calificación");
        }
        if (request.ratingEligible() != null) {
            employee.setRatingEligible(request.ratingEligible());
        }
        if (request.photoUrl() != null) {
            employee.setPhotoUrl(blankToNull(request.photoUrl()));
        }
        return toAdminParticipant(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<TrainerRatingMonthlySummary> monthlySummary(int year, int month) {
        List<Object[]> rows = trainerRatingRepository.findMonthlyRanking(year, month);
        List<TrainerRatingMonthlySummary> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            Long employeeId = (Long) row[0];
            String firstName = (String) row[1];
            String lastName = (String) row[2];
            String photoUrl = (String) row[3];
            double average = ((Number) row[4]).doubleValue();
            long count = ((Number) row[5]).longValue();
            result.add(new TrainerRatingMonthlySummary(
                    employeeId,
                    firstName + " " + lastName,
                    photoUrl,
                    Math.round(average * 10.0) / 10.0,
                    count,
                    rank++));
        }
        return result;
    }

    private TrainerRatingParticipantResponse toParticipantResponse(Employee employee) {
        return new TrainerRatingParticipantResponse(
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                employee.getPhotoUrl());
    }

    private RatingParticipantAdminResponse toAdminParticipant(Employee employee) {
        return new RatingParticipantAdminResponse(
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                employee.getRole(),
                employee.getRole().displayLabel(),
                employee.getActive(),
                employee.getRatingEligible(),
                employee.getPhotoUrl());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean canParticipateInRating(UserRole role) {
        return role != null && RATING_PARTICIPANT_ROLES.contains(role);
    }

    private static String normalizeIdentification(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Indica tu número de identificación");
        }
        String normalized = value.trim().replaceAll("[\\s.-]", "");
        if (normalized.length() < 5) {
            throw new BusinessException("El número de identificación no es válido");
        }
        return normalized;
    }
}
