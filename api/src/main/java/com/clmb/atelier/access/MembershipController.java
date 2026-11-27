package com.clmb.atelier.access;

import com.clmb.atelier.access.entities.AccessPlan;
import com.clmb.atelier.access.entities.MemberPass;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.clmb.atelier.config.JwtAccount.accountId;

@RestController
@RequestMapping("/api")
public class MembershipController {
    private final MembershipService memberships;

    public MembershipController(MembershipService memberships) {
        this.memberships = memberships;
    }

    @PostMapping("/organizations/{organizationId}/access-plans")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResponse createPlan(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID organizationId,
                                   @RequestBody PlanRequest request) {
        return PlanResponse.from(memberships.createPlan(accountId(jwt), organizationId, request.name(),
                request.gymId(), request.planType(), request.validUntil(), request.visitLimit()));
    }

    @GetMapping("/organizations/{organizationId}/access-plans")
    public List<PlanResponse> listPlans(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID organizationId) {
        return memberships.listPlans(accountId(jwt), organizationId).stream().map(PlanResponse::from).toList();
    }

    @DeleteMapping("/organizations/{organizationId}/access-plans/{planId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archivePlan(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID organizationId,
                            @PathVariable UUID planId) {
        memberships.archivePlan(accountId(jwt), organizationId, planId);
    }

    @PostMapping("/organizations/{organizationId}/access-plans/{planId}/passes")
    @ResponseStatus(HttpStatus.CREATED)
    public PassResponse issuePass(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID organizationId,
                                  @PathVariable UUID planId, @RequestBody IssuePassRequest request) {
        return PassResponse.from(memberships.issuePass(accountId(jwt), organizationId, planId, request.accountId(),
                request.startsAt(), request.expiresAt()));
    }

    @PostMapping("/gyms/{gymId}/access-plans/{planId}/passes")
    @ResponseStatus(HttpStatus.CREATED)
    public PassResponse issuePassAtGym(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId,
                                       @PathVariable UUID planId, @RequestBody IssuePassRequest request) {
        return PassResponse.from(memberships.issuePassAtGym(accountId(jwt), gymId, planId, request.accountId(),
                request.startsAt(), request.expiresAt()));
    }

    @DeleteMapping("/organizations/{organizationId}/member-passes/{passId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePass(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID organizationId,
                           @PathVariable UUID passId) {
        memberships.revokePass(accountId(jwt), organizationId, passId);
    }

    @GetMapping("/me/member-passes")
    public List<PassResponse> listOwnPasses(@AuthenticationPrincipal Jwt jwt) {
        return memberships.listMemberPasses(accountId(jwt)).stream().map(PassResponse::from).toList();
    }

    @ExceptionHandler(PassIssuanceConflict.class)
    ProblemDetail passConflict(PassIssuanceConflict conflict) {
        return conflict.getBody();
    }

    public record PlanRequest(String name, UUID gymId, AccessPlan.PlanType planType, Instant validUntil,
                              Integer visitLimit) {}
    public record IssuePassRequest(UUID accountId, Instant startsAt, Instant expiresAt) {}

    public record PlanResponse(UUID id, UUID organizationId, UUID gymId, String name, AccessPlan.PlanType planType,
                               Instant validUntil, Integer visitLimit, Instant archivedAt) {
        static PlanResponse from(AccessPlan plan) {
            return new PlanResponse(plan.getId(), plan.getOrganizationId(), plan.getGymId(), plan.getName(),
                    plan.getPlanType(), plan.getValidUntil(), plan.getVisitLimit(), plan.getArchivedAt());
        }
    }

    public record PassResponse(UUID id, UUID accessPlanId, UUID accountId, Instant startsAt, Instant expiresAt,
                               Instant revokedAt) {
        static PassResponse from(MemberPass pass) {
            return new PassResponse(pass.getId(), pass.getAccessPlanId(), pass.getAccountId(), pass.getStartsAt(),
                    pass.getExpiresAt(), pass.getRevokedAt());
        }
    }
}
