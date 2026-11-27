package com.clmb.atelier.grading;

import com.clmb.atelier.grading.dtos.GradeRequest;
import com.clmb.atelier.grading.dtos.GradeResponse;
import com.clmb.atelier.grading.dtos.GradingSystemRequest;
import com.clmb.atelier.grading.dtos.GradingSystemResponse;
import com.clmb.atelier.grading.dtos.GymGradingSystem;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.clmb.atelier.config.JwtUser.userId;

@RestController
@RequestMapping("/api/gyms/{gymId}")
public class GradingController {
    private final GradingService grading;

    public GradingController(GradingService grading) {
        this.grading = grading;
    }

    @GetMapping("/grading-systems")
    public List<GradingSystemResponse> listSystems(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId) {
        return grading.listSystems(userId(jwt), gymId);
    }

    @PostMapping("/grading-systems")
    @ResponseStatus(HttpStatus.CREATED)
    public GradingSystemResponse createGymSystem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId,
                                                 @RequestBody GradingSystemRequest request) {
        return grading.createGymSystem(userId(jwt), gymId, request);
    }

    @PostMapping("/grading-systems/shared")
    @ResponseStatus(HttpStatus.CREATED)
    public GradingSystemResponse createSharedSystem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId,
                                                    @RequestBody GradingSystemRequest request) {
        return grading.createSharedSystem(userId(jwt), gymId, request);
    }

    @GetMapping("/grading-system")
    public GymGradingSystem getSelectedSystem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId) {
        return grading.getSelectedSystem(userId(jwt), gymId);
    }

    @PutMapping("/grading-system")
    public GymGradingSystem selectSystem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId,
                                         @RequestBody GymGradingSystem request) {
        return grading.selectSystem(userId(jwt), gymId, request.gradingSystemId());
    }

    @PostMapping("/grading-systems/{systemId}/grades")
    @ResponseStatus(HttpStatus.CREATED)
    public GradeResponse addGrade(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gymId,
                                  @PathVariable UUID systemId, @RequestBody GradeRequest request) {
        return grading.addGrade(userId(jwt), gymId, systemId, request);
    }
}
