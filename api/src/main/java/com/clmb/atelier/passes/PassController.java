package com.clmb.atelier.passes;

import com.clmb.atelier.passes.entities.Pass;
import com.clmb.atelier.passes.entities.UserPass;
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
public class PassController {
    private final PassService passes;

    public PassController(PassService passes) {
        this.passes = passes;
    }

    @PostMapping("/organizations/{organizationId}/passes")
    @ResponseStatus(HttpStatus.CREATED)
    public PassResponse createPass(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID organizationId,
                                   @RequestBody PassRequest request) {
        return PassResponse.from(passes.createPass(accountId(jwt), organizationId, request.name(),
                request.gymId(), request.passType(), request.validUntil(), request.visitLimit()));
    }

    @GetMapping("/organizations/{organizationId}/passes")
    public List<PassResponse> listPasses(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID organizationId) {
        return passes.listPasses(accountId(jwt), organizationId).stream().map(PassResponse::from).toList();
    }

    @DeleteMapping("/organizations/{organizationId}/passes/{passId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archivePass(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID organizationId,
                            @PathVariable UUID passId) {
        passes.archivePass(accountId(jwt), organizationId, passId);
    }

    @PostMapping("/organizations/{organizationId}/passes/{passId}/user-passes")
    @ResponseStatus(HttpStatus.CREATED)
    public UserPassResponse issueUserPass(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID organizationId,
                                  @PathVariable UUID passId, @RequestBody IssueUserPassRequest request) {
        return UserPassResponse.from(passes.issueUserPass(accountId(jwt), organizationId, passId, request.accountId(),
                request.startsAt(), request.expiresAt()));
    }

    @PostMapping("/gyms/{gymId}/passes/{passId}/user-passes")
    @ResponseStatus(HttpStatus.CREATED)
    public UserPassResponse issueUserPassAtGym(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId,
                                       @PathVariable UUID passId, @RequestBody IssueUserPassRequest request) {
        return UserPassResponse.from(passes.issueUserPassAtGym(accountId(jwt), gymId, passId, request.accountId(),
                request.startsAt(), request.expiresAt()));
    }

    @DeleteMapping("/organizations/{organizationId}/user-passes/{userPassId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeUserPass(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID organizationId,
                           @PathVariable UUID userPassId) {
        passes.revokeUserPass(accountId(jwt), organizationId, userPassId);
    }

    @GetMapping("/me/user-passes")
    public List<UserPassResponse> listOwnUserPasses(@AuthenticationPrincipal Jwt jwt) {
        return passes.listUserPasses(accountId(jwt)).stream().map(UserPassResponse::from).toList();
    }

    @ExceptionHandler(PassIssuanceConflict.class)
    ProblemDetail passConflict(PassIssuanceConflict conflict) {
        return conflict.getBody();
    }

    public record PassRequest(String name, UUID gymId, Pass.PassType passType, Instant validUntil,
                              Integer visitLimit) {}
    public record IssueUserPassRequest(UUID accountId, Instant startsAt, Instant expiresAt) {}

    public record PassResponse(UUID id, UUID organizationId, UUID gymId, String name, Pass.PassType passType,
                               Instant validUntil, Integer visitLimit, Instant archivedAt) {
        static PassResponse from(Pass pass) {
            return new PassResponse(pass.getId(), pass.getOrganizationId(), pass.getGymId(), pass.getName(),
                    pass.getPassType(), pass.getValidUntil(), pass.getVisitLimit(), pass.getArchivedAt());
        }
    }

    public record UserPassResponse(UUID id, UUID passId, UUID accountId, Instant startsAt, Instant expiresAt,
                               Instant revokedAt) {
        static UserPassResponse from(UserPass userPass) {
            return new UserPassResponse(userPass.getId(), userPass.getPassId(), userPass.getAccountId(), userPass.getStartsAt(),
                    userPass.getExpiresAt(), userPass.getRevokedAt());
        }
    }
}
