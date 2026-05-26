package com.tacticaldistrict.command.dashboard.application.port;

import com.tacticaldistrict.command.dashboard.dto.AuditEventDto;
import java.util.List;

public interface DashboardRepositoryPort {

    List<Long> formationIds();

    List<Long> unitIds();

    List<Long> subdivisionIds();

    List<Long> personnelIds();

    List<Long> buildingIds();

    List<InventoryQuantityRow> equipmentQuantities();

    List<InventoryQuantityRow> weaponQuantities();

    List<AuditEventDto> latestEvents(int limit);

    record InventoryQuantityRow(Long unitId, Long quantity) {
    }
}
