package com.tacticaldistrict.command.dashboard.domain;

import com.tacticaldistrict.command.alert.dto.TacticalAlertDto;
import com.tacticaldistrict.command.dashboard.dto.DashboardStatisticsDto;
import com.tacticaldistrict.command.dashboard.dto.ReadinessAxisDto;
import com.tacticaldistrict.command.dashboard.dto.ReadinessDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReadinessCalculator {

    public ReadinessDto calculate(DashboardStatisticsDto stats, List<TacticalAlertDto> alerts) {
        long unitsWithoutEquipment = count(alerts, "UNIT_WITHOUT_EQUIPMENT");
        long unitsWithoutWeapons = count(alerts, "UNIT_WITHOUT_WEAPONS");
        long specialtyGaps = count(alerts, "SPECIALTY_WITHOUT_SPECIALISTS");
        long infraAlerts = count(alerts, "BUILDING_WITHOUT_SUBDIVISIONS") + count(alerts, "BUILDING_OVERLOADED");
        long critical = alerts.stream().filter(alert -> "CRITICAL".equals(alert.severity())).count();

        int personnel = clamp(stats.personnel() == 0 ? 0 : 85 - (int) Math.min(30, specialtyGaps * 4));
        int equipment = clamp(stats.units() == 0 ? 0 : 100 - (int) Math.min(70, unitsWithoutEquipment * 25 + critical * 5));
        int weapons = clamp(stats.units() == 0 ? 0 : 100 - (int) Math.min(70, unitsWithoutWeapons * 25 + critical * 5));
        int specialists = clamp(90 - (int) Math.min(60, specialtyGaps * 12));
        int infrastructure = clamp(stats.buildings() == 0 ? 0 : 92 - (int) Math.min(60, infraAlerts * 10));
        int overall = clamp((personnel + equipment + weapons + specialists + infrastructure) / 5);

        return new ReadinessDto(overall, List.of(
                axis("personnel", "Личный состав", personnel),
                axis("equipment", "Техника", equipment),
                axis("weapons", "Вооружение", weapons),
                axis("specialists", "Специалисты", specialists),
                axis("infrastructure", "Инфраструктура", infrastructure)
        ));
    }

    private long count(List<TacticalAlertDto> alerts, String type) {
        return alerts.stream().filter(alert -> type.equals(alert.type())).count();
    }

    private ReadinessAxisDto axis(String key, String label, int score) {
        return new ReadinessAxisDto(key, label, score, score >= 80 ? "READY" : score >= 55 ? "WATCH" : "CRITICAL");
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
