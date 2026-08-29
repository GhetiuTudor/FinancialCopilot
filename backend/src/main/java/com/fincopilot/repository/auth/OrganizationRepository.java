package com.fincopilot.repository.auth;

import com.fincopilot.entity.auth.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


//la toate interfetele care extind jpa -> findBy<Field><Operator><And/Or><Field><Operator>OrderBy<Field><Asc/Desc>

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
