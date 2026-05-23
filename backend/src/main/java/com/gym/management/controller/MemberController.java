package com.gym.management.controller;

import com.gym.management.dto.AdminSetMemberPasswordRequest;
import com.gym.management.dto.MemberBulkDeleteResponse;
import com.gym.management.dto.MemberImportResponse;
import com.gym.management.dto.MemberRequest;
import com.gym.management.dto.MemberResponse;
import com.gym.management.service.MemberImportService;
import com.gym.management.service.MemberPortalService;
import com.gym.management.service.MemberService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberImportService memberImportService;
    private final MemberPortalService memberPortalService;

    @GetMapping
    public List<MemberResponse> findAll() {
        return memberService.findAll();
    }

    @GetMapping("/{id}")
    public MemberResponse findById(@PathVariable Long id) {
        return memberService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberRequest request) {
        return memberService.create(request);
    }

    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return memberService.update(id, request);
    }

    @PostMapping("/{id}/freeze-membership")
    public MemberResponse freezeMembership(@PathVariable Long id) {
        return memberService.freezeMembership(id);
    }

    @PostMapping("/{id}/unfreeze-membership")
    public MemberResponse unfreezeMembership(@PathVariable Long id) {
        return memberService.unfreezeMembership(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        memberService.delete(id);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MemberBulkDeleteResponse deleteAll() {
        return memberService.deleteAll();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MemberImportResponse importFromExcel(@RequestParam("file") MultipartFile file) {
        return memberImportService.importFromExcel(file);
    }

    @PutMapping("/{id}/portal-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPortalPassword(@PathVariable Long id, @Valid @RequestBody AdminSetMemberPasswordRequest request) {
        memberPortalService.setPasswordByAdmin(id, request);
    }

    @PostMapping("/{id}/portal-password/reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPortalPassword(@PathVariable Long id) {
        memberPortalService.resetPasswordToDocument(id);
    }
}
