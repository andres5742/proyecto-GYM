package com.gym.management.controller;

import com.gym.management.dto.AttendanceSummaryResponse;
import com.gym.management.dto.WorkAttendanceRequest;
import com.gym.management.dto.WorkAttendanceResponse;
import com.gym.management.service.WorkAttendanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class WorkAttendanceController {

    private final WorkAttendanceService attendanceService;

    @GetMapping
    public List<WorkAttendanceResponse> findAll(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.findAll(employeeId, year, month, date);
    }

    @GetMapping("/summary")
    public AttendanceSummaryResponse getSummary(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.getSummary(employeeId, year, month, date);
    }

    @PostMapping("/clock-in/{employeeId}")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkAttendanceResponse clockIn(@PathVariable Long employeeId) {
        return attendanceService.clockIn(employeeId);
    }

    @PostMapping("/{id}/clock-out")
    public WorkAttendanceResponse clockOut(@PathVariable Long id) {
        return attendanceService.clockOut(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkAttendanceResponse create(@Valid @RequestBody WorkAttendanceRequest request) {
        return attendanceService.create(request);
    }

    @PutMapping("/{id}")
    public WorkAttendanceResponse update(
            @PathVariable Long id, @Valid @RequestBody WorkAttendanceRequest request) {
        return attendanceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        attendanceService.delete(id);
    }
}
