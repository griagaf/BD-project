package com.tacticaldistrict.command.alert.controller;

import com.tacticaldistrict.command.alert.dto.TacticalAlertDto;
import com.tacticaldistrict.command.alert.service.AlertService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public List<TacticalAlertDto> alerts() {
        return alertService.alerts();
    }
}
