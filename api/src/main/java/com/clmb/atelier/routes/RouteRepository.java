package com.clmb.atelier.routes;

import com.clmb.atelier.routes.dtos.RouteListItem;
import com.clmb.atelier.routes.entities.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RouteRepository extends JpaRepository<Route, UUID> {
    @Query("""
            select new com.clmb.atelier.routes.dtos.RouteListItem(
                route.id, wall.id, wall.name, wall.discipline, route.name, route.gradeId, grade.label, route.setAt)
            from Route route, Wall wall, Grade grade
            where route.wallId = wall.id and wall.gymId = :gymId and route.gradeId = grade.id
                and route.removedAt is null and wall.archivedAt is null
            order by route.setAt desc, route.id
            """)
    List<RouteListItem> findActiveByGymId(@Param("gymId") UUID gymId);
}
