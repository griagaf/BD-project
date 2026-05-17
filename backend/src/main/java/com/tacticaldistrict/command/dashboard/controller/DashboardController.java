package com.tacticaldistrict.command.dashboard.controller;

import com.tacticaldistrict.command.dashboard.dto.ReadinessDto;
import com.tacticaldistrict.command.dashboard.dto.TacticalDashboardDto;
import com.tacticaldistrict.command.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public TacticalDashboardDto dashboard() {
        return dashboardService.dashboard();
    }

    @GetMapping("/readiness")
    public ReadinessDto readiness() {
        return dashboardService.readiness();
    }
}
