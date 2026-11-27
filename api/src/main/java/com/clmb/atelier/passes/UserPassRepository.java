package com.clmb.atelier.passes;

import com.clmb.atelier.passes.entities.Pass;
import com.clmb.atelier.passes.entities.UserPass;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPassRepository extends JpaRepository<UserPass, UUID> {
    List<UserPass> findAllByAccountIdOrderByExpiresAtDesc(UUID accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select userPass from UserPass userPass
            where userPass.id = :id and exists (
                select pass.id from Pass pass
                where pass.id = userPass.passId and pass.organizationId = :organizationId
            )
            """)
    Optional<UserPass> findInOrganizationForUpdate(@Param("id") UUID id, @Param("organizationId") UUID organizationId);

    @Query("""
            select userPass.id as id, pass.passType as passType
            from UserPass userPass, Pass pass
            where userPass.passId = pass.id and pass.organizationId = :organizationId
                and userPass.accountId = :accountId and userPass.revokedAt is null
                and userPass.startsAt < :before and userPass.expiresAt > :after
                and (pass.passType = :membershipType or pass.visitLimit is null or pass.visitLimit >
                    (select count(checkIn) from CheckIn checkIn where checkIn.userPassId = userPass.id))
            order by userPass.id
            """)
    List<OverlappingUserPass> findUsableOverlaps(@Param("organizationId") UUID organizationId,
                                             @Param("accountId") UUID accountId,
                                             @Param("after") Instant after, @Param("before") Instant before,
                                             @Param("membershipType") Pass.PassType membershipType);

    interface OverlappingUserPass {
        UUID getId();

        Pass.PassType getPassType();
    }
}
