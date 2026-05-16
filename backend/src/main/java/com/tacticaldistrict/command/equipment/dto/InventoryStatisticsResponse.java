package com.tacticaldistrict.command.equipment.dto;

public record InventoryStatisticsResponse(
        Long visibleUnits,
        Long inventoryRows,
        Long totalQuantity,
        Long warningRows,
        Integer readinessScore
) {
}
