package com.gym.management.controller;

import com.gym.management.dto.MemberProgressEntryRequest;
import com.gym.management.dto.MemberProgressEntryResponse;
import com.gym.management.dto.UploadResponse;
import com.gym.management.service.FileStorageService;
import com.gym.management.service.MemberProgressService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/member-portal/progress")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AFFILIATE')")
public class MemberPortalProgressController {

    private final MemberProgressService memberProgressService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public List<MemberProgressEntryResponse> list() {
        return memberProgressService.listMine();
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse uploadImage(@RequestParam("file") MultipartFile file) {
        return new UploadResponse(fileStorageService.storeImage(file));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberProgressEntryResponse create(@Valid @RequestBody MemberProgressEntryRequest request) {
        return memberProgressService.create(request);
    }

    @PutMapping("/{id}")
    public MemberProgressEntryResponse update(
            @PathVariable Long id, @Valid @RequestBody MemberProgressEntryRequest request) {
        return memberProgressService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        memberProgressService.delete(id);
    }
}
