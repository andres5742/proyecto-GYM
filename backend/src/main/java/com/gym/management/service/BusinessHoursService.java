package com.gym.management.service;

import com.gym.management.dto.BusinessDayScheduleDto;
import com.gym.management.dto.BusinessHoursResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.mapper.BusinessHoursMapper;
import com.gym.management.model.BusinessDaySchedule;
import com.gym.management.repository.BusinessDayScheduleRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessHoursService {

    private final BusinessDayScheduleRepository scheduleRepository;

    @Transactional
    public BusinessHoursResponse get() {
        ensureDefaults();
        return BusinessHoursMapper.toResponse(scheduleRepository.findAll());
    }

    @Transactional
    public BusinessHoursResponse update(List<BusinessDayScheduleDto> days) {
        ensureDefaults();
        if (days == null || days.size() != 7) {
            throw new BusinessException("Debe configurar los 7 días de la semana");
        }
        for (BusinessDayScheduleDto dto : days) {
            validateDay(dto);
            BusinessDaySchedule schedule = scheduleRepository
                    .findById(dto.dayOfWeek())
                    .orElseGet(() -> BusinessDaySchedule.builder()
                            .dayOfWeek(dto.dayOfWeek())
                            .build());
            boolean closed = Boolean.TRUE.equals(dto.closed());
            schedule.setClosed(closed);
            schedule.setOpenTime(closed ? null : dto.openTime());
            schedule.setCloseTime(closed ? null : dto.closeTime());
            scheduleRepository.save(schedule);
        }
        return get();
    }

    private void validateDay(BusinessDayScheduleDto dto) {
        if (dto.dayOfWeek() == null) {
            throw new BusinessException("Día de la semana inválido");
        }
        if (Boolean.TRUE.equals(dto.closed())) {
            return;
        }
        if (dto.openTime() == null || dto.closeTime() == null) {
            throw new BusinessException("Indique hora de apertura y cierre para " + dayLabel(dto.dayOfWeek()));
        }
        if (!dto.openTime().isBefore(dto.closeTime())) {
            throw new BusinessException("La hora de apertura debe ser anterior al cierre (" + dayLabel(dto.dayOfWeek()) + ")");
        }
    }

    private void ensureDefaults() {
        if (scheduleRepository.count() == 7) {
            return;
        }
        for (DayOfWeek day : DayOfWeek.values()) {
            if (scheduleRepository.existsById(day)) {
                continue;
            }
            boolean sunday = day == DayOfWeek.SUNDAY;
            boolean saturday = day == DayOfWeek.SATURDAY;
            scheduleRepository.save(BusinessDaySchedule.builder()
                    .dayOfWeek(day)
                    .closed(false)
                    .openTime(sunday ? LocalTime.of(7, 0) : saturday ? LocalTime.of(6, 0) : LocalTime.of(5, 0))
                    .closeTime(sunday ? LocalTime.of(14, 0) : saturday ? LocalTime.of(20, 0) : LocalTime.of(22, 0))
                    .build());
        }
    }

    private String dayLabel(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miércoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }

    public static List<DayOfWeek> allDays() {
        return Arrays.asList(DayOfWeek.values());
    }
}
