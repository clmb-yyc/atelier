package com.clmb.atelier.routes;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations/{locationId}/routes")
public class RoutesController {
    public RoutesController() {}

    @GetMapping()
    public String getRoute() {
        return "hello world";
    }
}
