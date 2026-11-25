package com.clmb.atelier.access;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

class PassIssuanceConflict extends ResponseStatusException {
    PassIssuanceConflict(String code, String detail, List<UUID> passIds) {
        super(HttpStatus.CONFLICT, detail);
        getBody().setProperty("code", code);
        getBody().setProperty("conflictingPassIds", passIds);
    }
}
