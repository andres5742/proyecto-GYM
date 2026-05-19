package com.gym.management.controller;

import com.gym.management.dto.FeedbackMessageRequest;
import com.gym.management.dto.FeedbackMessageResponse;
import com.gym.management.dto.FeedbackStatusUpdateRequest;
import com.gym.management.dto.UploadResponse;
import com.gym.management.service.FeedbackMessageService;
import com.gym.management.service.FileStorageService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackMessageController {

    private final FeedbackMessageService feedbackMessageService;
    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse uploadImage(@RequestParam("file") MultipartFile file) {
        return new UploadResponse(fileStorageService.storeImage(file));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackMessageResponse create(@Valid @RequestBody FeedbackMessageRequest request) {
        return feedbackMessageService.create(request);
    }

    @GetMapping
    public List<FeedbackMessageResponse> findAll() {
        return feedbackMessageService.findAllForAdmin();
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of("pending", feedbackMessageService.countPending());
    }

    @PatchMapping("/{id}/status")
    public FeedbackMessageResponse updateStatus(
            @PathVariable Long id, @Valid @RequestBody FeedbackStatusUpdateRequest request) {
        return feedbackMessageService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void delete(@PathVariable Long id) {
        feedbackMessageService.delete(id);
    }
}
