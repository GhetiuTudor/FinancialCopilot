package com.fincopilot.controller;

import com.fincopilot.dto.profile.CompanyProfileResponse;
import com.fincopilot.dto.profile.UpdateCompanyProfileRequest;
import com.fincopilot.security.CurrentUser;
import com.fincopilot.service.CompanyProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;

    @GetMapping
    public ResponseEntity<CompanyProfileResponse> getProfile() {
        CompanyProfileResponse response = companyProfileService.getOrCreate(CurrentUser.getOrgId());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<CompanyProfileResponse> updateProfile(@RequestBody UpdateCompanyProfileRequest request) {
        CompanyProfileResponse response = companyProfileService.update(CurrentUser.getOrgId(), request);
        return ResponseEntity.ok(response);
    }
}
