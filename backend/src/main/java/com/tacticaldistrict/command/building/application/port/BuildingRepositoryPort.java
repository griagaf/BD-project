package com.tacticaldistrict.command.building.application.port;

import com.tacticaldistrict.command.building.dto.BuildingFilter;
import com.tacticaldistrict.command.building.dto.BuildingRequest;
import com.tacticaldistrict.command.building.dto.BuildingResponse;
import java.util.List;
import java.util.Optional;

public interface BuildingRepositoryPort {

    List<BuildingResponse> search(BuildingFilter filter);

    Optional<BuildingResponse> findById(Long id);

    Long create(BuildingRequest request, boolean assignable);

    boolean update(Long id, BuildingRequest request, boolean assignable);

    boolean delete(Long id);

    Optional<Long> subdivisionUnitId(Long subdivisionId);

    Optional<Long> currentBuildingId(Long subdivisionId);

    void assignSubdivision(Long buildingId, Long subdivisionId);

    void removeSubdivision(Long buildingId, Long subdivisionId);
}
