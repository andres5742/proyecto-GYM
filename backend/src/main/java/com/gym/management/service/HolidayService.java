package com.gym.management.service;

import com.gym.management.dto.HolidayRequest;
import com.gym.management.dto.HolidayResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.HolidayMapper;
import com.gym.management.model.Holiday;
import com.gym.management.repository.HolidayRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;

    @Transactional(readOnly = true)
    public List<HolidayResponse> findBetween(LocalDate from, LocalDate to) {
        return holidayRepository.findByDateBetweenOrderByDateAsc(from, to).stream()
                .map(HolidayMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HolidayResponse> findByYearMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return findBetween(ym.atDay(1), ym.atEndOfMonth());
    }

    @Transactional
    public HolidayResponse create(HolidayRequest request) {
        if (holidayRepository.existsByDate(request.date())) {
            throw new BusinessException("Ya existe un festivo registrado para esa fecha");
        }
        Holiday holiday = Holiday.builder()
                .date(request.date())
                .name(request.name().trim())
                .description(blankToNull(request.description()))
                .build();
        return HolidayMapper.toResponse(holidayRepository.save(holiday));
    }

    @Transactional
    public HolidayResponse update(Long id, HolidayRequest request) {
        Holiday holiday = getHoliday(id);
        if (holidayRepository.existsByDateAndIdNot(request.date(), id)) {
            throw new BusinessException("Ya existe un festivo registrado para esa fecha");
        }
        holiday.setDate(request.date());
        holiday.setName(request.name().trim());
        holiday.setDescription(blankToNull(request.description()));
        return HolidayMapper.toResponse(holidayRepository.save(holiday));
    }

    @Transactional
    public void delete(Long id) {
        if (!holidayRepository.existsById(id)) {
            throw new ResourceNotFoundException("Festivo no encontrado: " + id);
        }
        holidayRepository.deleteById(id);
    }

    private Holiday getHoliday(Long id) {
        return holidayRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Festivo no encontrado: " + id));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
