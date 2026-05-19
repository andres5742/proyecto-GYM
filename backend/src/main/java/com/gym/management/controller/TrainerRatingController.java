package com.gym.management.controller;

import com.gym.management.dto.RatingParticipantAdminResponse;
import com.gym.management.dto.RatingParticipantUpdateRequest;
import com.gym.management.dto.TrainerRatingMonthlySummary;
import com.gym.management.dto.TrainerRatingParticipantResponse;
import com.gym.management.dto.TrainerRatingSubmitRequest;
import com.gym.management.service.TrainerRatingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trainer-ratings")
@RequiredArgsConstructor
public class TrainerRatingController {

    private final TrainerRatingService trainerRatingService;

    @GetMapping("/participants")
    public List<TrainerRatingParticipantResponse> publicParticipants() {
        return trainerRatingService.findPublicParticipants();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void submit(@Valid @RequestBody TrainerRatingSubmitRequest request) {
        trainerRatingService.submit(request);
    }

    @GetMapping("/participants/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<RatingParticipantAdminResponse> allParticipantsForConfig() {
        return trainerRatingService.findAllForConfig();
    }

    @PatchMapping("/participants/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public RatingParticipantAdminResponse updateParticipant(
            @PathVariable Long id, @Valid @RequestBody RatingParticipantUpdateRequest request) {
        return trainerRatingService.updateParticipant(id, request);
    }

    @GetMapping("/monthly")
    public List<TrainerRatingMonthlySummary> monthly(
            @RequestParam int year, @RequestParam int month) {
        return trainerRatingService.monthlySummary(year, month);
    }
}
