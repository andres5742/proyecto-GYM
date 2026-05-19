package com.gym.management.controller;

import com.gym.management.dto.MembershipPlanRequest;
import com.gym.management.dto.MembershipPlanResponse;
import com.gym.management.service.MembershipPlanService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class MembershipPlanController {

    private final MembershipPlanService planService;

    @GetMapping
    public List<MembershipPlanResponse> findAll() {
        return planService.findAll();
    }

    @GetMapping("/{id}")
    public MembershipPlanResponse findById(@PathVariable Long id) {
        return planService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipPlanResponse create(@Valid @RequestBody MembershipPlanRequest request) {
        return planService.create(request);
    }

    @PutMapping("/{id}")
    public MembershipPlanResponse update(
            @PathVariable Long id, @Valid @RequestBody MembershipPlanRequest request) {
        return planService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        planService.delete(id);
    }
}
