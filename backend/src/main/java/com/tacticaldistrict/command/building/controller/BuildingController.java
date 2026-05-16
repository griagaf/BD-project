package com.tacticaldistrict.command.building.controller;

import com.tacticaldistrict.command.building.dto.BuildingFilter;
import com.tacticaldistrict.command.building.dto.BuildingRequest;
import com.tacticaldistrict.command.building.dto.BuildingResponse;
import com.tacticaldistrict.command.building.dto.BuildingStatisticsResponse;
import com.tacticaldistrict.command.building.service.BuildingService;
import com.tacticaldistrict.command.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;

    @GetMapping("/api/buildings")
    public PageResponse<BuildingResponse> search(
            @ParameterObject BuildingFilter filter,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return buildingService.search(filter, pageable);
    }

    @GetMapping("/api/buildings/statistics")
    public BuildingStatisticsResponse statistics() {
        return buildingService.statistics();
    }

    @GetMapping("/api/buildings/{id}")
    public BuildingResponse findById(@PathVariable Long id) {
        return buildingService.findById(id);
    }

    @GetMapping("/api/units/{unitId}/buildings")
    public List<BuildingResponse> byUnit(@PathVariable Long unitId) {
        return buildingService.byUnit(unitId);
    }

    @PostMapping("/api/buildings")
    @ResponseStatus(HttpStatus.CREATED)
    public BuildingResponse create(@Valid @RequestBody BuildingRequest request) {
        return buildingService.create(request);
    }

    @PutMapping("/api/buildings/{id}")
    public BuildingResponse update(
            @PathVariable Long id,
            @Valid @RequestBody BuildingRequest request
    ) {
        return buildingService.update(id, request);
    }

    @DeleteMapping("/api/buildings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        buildingService.delete(id);
    }

    @PostMapping("/api/buildings/{id}/subdivisions/{subdivisionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignSubdivision(
            @PathVariable Long id,
            @PathVariable Long subdivisionId
    ) {
        buildingService.assignSubdivision(id, subdivisionId);
    }

    @DeleteMapping("/api/buildings/{id}/subdivisions/{subdivisionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSubdivision(
            @PathVariable Long id,
            @PathVariable Long subdivisionId
    ) {
        buildingService.removeSubdivision(id, subdivisionId);
    }
}
