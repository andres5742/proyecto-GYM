package com.gym.management.service;

import com.gym.management.dto.WorkShiftRequest;
import com.gym.management.dto.WorkShiftResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.WorkShiftMapper;
import com.gym.management.model.ShiftStatus;
import com.gym.management.model.WorkShift;
import com.gym.management.repository.WorkShiftRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkShiftService {

    private final WorkShiftRepository workShiftRepository;

    @Transactional(readOnly = true)
    public List<WorkShiftResponse> findAll() {
        return workShiftRepository.findAllByOrderByShiftDateDescOpenedAtDesc().stream()
                .map(WorkShiftMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkShiftResponse findOpen() {
        return workShiftRepository.findFirstByStatusOrderByOpenedAtDesc(ShiftStatus.OPEN)
                .map(WorkShiftMapper::toResponse)
                .orElse(null);
    }

    @Transactional
    public WorkShiftResponse open(WorkShiftRequest request) {
        if (workShiftRepository.existsByStatus(ShiftStatus.OPEN)) {
            throw new BusinessException("Ya hay un turno abierto. Ciérrelo antes de abrir otro.");
        }
        LocalDate shiftDate = LocalDate.now();
        WorkShift shift = WorkShift.builder()
                .shiftDate(shiftDate)
                .name(request.name())
                .openedAt(LocalDateTime.now())
                .status(ShiftStatus.OPEN)
                .build();
        return WorkShiftMapper.toResponse(workShiftRepository.save(shift));
    }

    @Transactional
    public WorkShiftResponse close(Long id) {
        WorkShift shift = getShift(id);
        if (shift.getStatus() == ShiftStatus.CLOSED) {
            throw new BusinessException("El turno ya está cerrado");
        }
        shift.setStatus(ShiftStatus.CLOSED);
        shift.setClosedAt(LocalDateTime.now());
        return WorkShiftMapper.toResponse(workShiftRepository.save(shift));
    }

    public WorkShift getShift(Long id) {
        return workShiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + id));
    }

    public WorkShift getOpenShiftRequired() {
        return workShiftRepository.findFirstByStatusOrderByOpenedAtDesc(ShiftStatus.OPEN)
                .orElseThrow(() -> new BusinessException(
                        "No hay un turno abierto. Abra un turno antes de registrar ventas."));
    }
}
