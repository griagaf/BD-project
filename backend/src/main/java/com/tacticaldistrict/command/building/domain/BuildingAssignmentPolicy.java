package com.tacticaldistrict.command.building.domain;

import com.tacticaldistrict.command.building.dto.BuildingResponse;
import org.springframework.stereotype.Component;

@Component
public class BuildingAssignmentPolicy {

    public void validate(BuildingResponse building, Long subdivisionUnitId, Long currentBuildingId) {
        if (!building.unitId().equals(subdivisionUnitId)) {
            throw new IllegalArgumentException("Подразделение и сооружение должны относиться к одной военной части.");
        }
        if (!Boolean.TRUE.equals(building.assignable())) {
            throw new IllegalArgumentException("Невозможно назначить подразделение: выбранное сооружение не предназначено для размещения подразделений.");
        }
        if (currentBuildingId != null && !currentBuildingId.equals(building.id())) {
            throw new IllegalArgumentException("Подразделение уже размещено в другом сооружении. Сначала снимите текущее закрепление.");
        }
    }
}
