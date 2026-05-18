package com.tacticaldistrict.command.dashboard.dto;

public record DashboardStatisticsDto(
        Long formations,
        Long units,
        Long subdivisions,
        Long personnel,
        Long equipmentQuantity,
        Long weaponQuantity,
        Long buildings,
        Long openAlerts
) {
}
