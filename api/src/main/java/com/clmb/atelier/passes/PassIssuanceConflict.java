package com.clmb.atelier.passes;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

class PassIssuanceConflict extends ResponseStatusException {
    PassIssuanceConflict(String code, String detail, List<UUID> userPassIds) {
        super(HttpStatus.CONFLICT, detail);
        getBody().setProperty("code", code);
        getBody().setProperty("conflictingUserPassIds", userPassIds);
    }
}
