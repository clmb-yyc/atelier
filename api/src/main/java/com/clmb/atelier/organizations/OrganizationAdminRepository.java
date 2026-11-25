package com.clmb.atelier.organizations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrganizationAdminRepository extends JpaRepository<OrganizationAdmin, OrganizationAdmin.Key> {
    @Modifying
    @Query(value = """
            INSERT INTO organization_admins (organization_id, account_id)
            VALUES (:organizationId, :accountId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int grant(@Param("organizationId") UUID organizationId, @Param("accountId") UUID accountId);
}
