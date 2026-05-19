package com.gym.management.controller;

import com.gym.management.dto.ChangePasswordRequest;
import com.gym.management.dto.MemberPortalProfileResponse;
import com.gym.management.service.MemberPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member-portal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AFFILIATE')")
public class MemberPortalController {

    private final MemberPortalService memberPortalService;

    @GetMapping("/me")
    public MemberPortalProfileResponse me() {
        return memberPortalService.myProfile();
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        memberPortalService.changeOwnPassword(request);
    }
}
