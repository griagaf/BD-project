package com.tacticaldistrict.command.alert.application.port;

import com.tacticaldistrict.command.alert.repository.AlertQueryRepository.BuildingCandidate;
import com.tacticaldistrict.command.alert.repository.AlertQueryRepository.InventoryCandidate;
import com.tacticaldistrict.command.alert.repository.AlertQueryRepository.SpecialtyCandidate;
import com.tacticaldistrict.command.alert.repository.AlertQueryRepository.UnitCandidate;
import java.util.List;

public interface AlertRepositoryPort {

    List<UnitCandidate> unitsWithoutEquipment();

    List<UnitCandidate> unitsWithoutWeapons();

    List<BuildingCandidate> buildingsWithoutSubdivisions();

    List<BuildingCandidate> overloadedBuildings(int threshold);

    List<SpecialtyCandidate> specialtiesWithoutSpecialists();

    List<InventoryCandidate> equipmentQuantityExceeded(int threshold);

    List<InventoryCandidate> weaponQuantityExceeded(int threshold);
}
