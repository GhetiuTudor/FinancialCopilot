package com.fincopilot.service;

import com.fincopilot.dto.profile.CompanyProfileResponse;
import com.fincopilot.dto.profile.UpdateCompanyProfileRequest;
import com.fincopilot.entity.CompanyProfile;
import com.fincopilot.entity.auth.Organization;
import com.fincopilot.repository.CompanyProfileRepository;
import com.fincopilot.repository.auth.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyProfileService {

    private final CompanyProfileRepository companyProfileRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public CompanyProfileResponse get(UUID orgId) {
        CompanyProfile profile = getOrCreateEntity(orgId);
        return toResponse(profile);
    }

    @Transactional
    public CompanyProfileResponse getOrCreate(UUID orgId) {
        CompanyProfile profile = getOrCreateEntity(orgId);
        return toResponse(profile);
    }

    @Transactional
    public CompanyProfileResponse update(UUID orgId, UpdateCompanyProfileRequest dto) {
        CompanyProfile profile = getOrCreateEntity(orgId);

        if (dto.getEntityType() != null) {
            profile.setEntityType(dto.getEntityType());
        }
        if (dto.getTaxRegime() != null) {
            profile.setTaxRegime(dto.getTaxRegime());
        }
        if (dto.getEmployeesCount() != null) {
            profile.setEmployeesCount(dto.getEmployeesCount());
        }
        if (dto.getVatRegistered() != null) {
            profile.setVatRegistered(dto.getVatRegistered());
        }
        if (dto.getVatCashAccounting() != null) {
            profile.setVatCashAccounting(dto.getVatCashAccounting());
        }
        // tva poate fi setata null
        profile.setVatPeriod(dto.getVatPeriod());

        if (dto.getEstimatedAnnualRevenue() != null) {
            profile.setEstimatedAnnualRevenue(dto.getEstimatedAnnualRevenue());
        }
        if (dto.getEstimatedAnnualExpenses() != null) {
            profile.setEstimatedAnnualExpenses(dto.getEstimatedAnnualExpenses());
        }
        if (dto.getNormValue() != null) {
            profile.setNormValue(dto.getNormValue());
        }

        profile.setCounty(dto.getCounty());
        profile.setCity(dto.getCity());

        if (dto.getActivityStartDate() != null) {
            profile.setActivityStartDate(LocalDate.parse(dto.getActivityStartDate()));
        }

        profile = companyProfileRepository.save(profile);
        log.info("Updated company profile for org: {}", orgId);

        return toResponse(profile);
    }

    // raw entity pt ai request
    @Transactional(readOnly = true)
    public CompanyProfile getEntityOrDefault(UUID orgId) {
        return companyProfileRepository.findByOrganizationId(orgId).orElse(null);
    }

    // helpers
    private CompanyProfile getOrCreateEntity(UUID orgId) {
        return companyProfileRepository.findByOrganizationId(orgId).orElseGet(() -> createDefault(orgId));
    }

    private CompanyProfile createDefault(UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + orgId));

        CompanyProfile profile = CompanyProfile.builder()
                .organization(org)
                .entityType("SRL")
                .taxRegime("MICRO")
                .employeesCount(0)
                .vatRegistered(false)
                .vatCashAccounting(false)
                .build();

        profile = companyProfileRepository.save(profile);
        log.info("Created default company profile for org: {}", orgId);
        return profile;
    }

    private CompanyProfileResponse toResponse(CompanyProfile p) {
        return CompanyProfileResponse.builder()
                .entityType(p.getEntityType())
                .taxRegime(p.getTaxRegime())
                .employeesCount(p.getEmployeesCount())
                .vatRegistered(p.isVatRegistered())
                .vatCashAccounting(p.isVatCashAccounting())
                .vatPeriod(p.getVatPeriod())
                .estimatedAnnualRevenue(p.getEstimatedAnnualRevenue())
                .estimatedAnnualExpenses(p.getEstimatedAnnualExpenses())
                .normValue(p.getNormValue())
                .county(p.getCounty())
                .city(p.getCity())
                .activityStartDate(p.getActivityStartDate() != null ? p.getActivityStartDate().toString() : null)
                .build();
    }
}
