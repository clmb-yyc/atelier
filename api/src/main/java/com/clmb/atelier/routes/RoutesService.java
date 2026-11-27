package com.clmb.atelier.routes;

import com.clmb.atelier.administration.AdministrationAccess;
import com.clmb.atelier.routes.dtos.RouteListItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RoutesService {
    private final RouteRepository routes;
    private final AdministrationAccess access;

    public RoutesService(RouteRepository routes, AdministrationAccess access) {
        this.routes = routes;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<RouteListItem> listActiveRoutes(UUID actorId, UUID gymId) {
        access.requireGymStaffOrAdmin(actorId, gymId);
        return routes.findActiveByGymId(gymId);
    }
}
