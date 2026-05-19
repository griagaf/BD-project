package com.tacticaldistrict.command.equipment.controller;

import com.tacticaldistrict.command.common.dto.PageResponse;
import com.tacticaldistrict.command.equipment.dto.EquipmentCategoryResponse;
import com.tacticaldistrict.command.equipment.dto.EquipmentFilter;
import com.tacticaldistrict.command.equipment.dto.EquipmentTypePassportResponse;
import com.tacticaldistrict.command.equipment.dto.EquipmentTypeRequest;
import com.tacticaldistrict.command.equipment.dto.InventoryCategoryRequest;
import com.tacticaldistrict.command.equipment.dto.EquipmentTypeResponse;
import com.tacticaldistrict.command.equipment.dto.InventoryQuantityRequest;
import com.tacticaldistrict.command.equipment.dto.InventoryStatisticsResponse;
import com.tacticaldistrict.command.equipment.dto.UnitEquipmentResponse;
import com.tacticaldistrict.command.equipment.service.EquipmentService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping("/api/equipment")
    public PageResponse<UnitEquipmentResponse> search(
            @ParameterObject EquipmentFilter filter,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return equipmentService.search(filter, pageable);
    }

    @GetMapping("/api/equipment/statistics")
    public InventoryStatisticsResponse statistics() {
        return equipmentService.statistics();
    }

    @GetMapping("/api/equipment/categories")
    public List<EquipmentCategoryResponse> categories() {
        return equipmentService.categories();
    }

    @GetMapping("/api/equipment/types")
    public List<EquipmentTypeResponse> types() {
        return equipmentService.types();
    }

    @GetMapping("/api/equipment/types/{id}/passport")
    public EquipmentTypePassportResponse typePassport(@PathVariable Long id) {
        return equipmentService.typePassport(id);
    }

    @PostMapping("/api/equipment/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentCategoryResponse createCategory(@Valid @RequestBody InventoryCategoryRequest request) {
        return equipmentService.createCategory(request);
    }

    @PutMapping("/api/equipment/categories/{id}")
    public EquipmentCategoryResponse updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody InventoryCategoryRequest request
    ) {
        return equipmentService.updateCategory(id, request);
    }

    @DeleteMapping("/api/equipment/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveCategory(@PathVariable Long id) {
        equipmentService.archiveCategory(id);
    }

    @PostMapping("/api/equipment/types")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentTypeResponse createType(@Valid @RequestBody EquipmentTypeRequest request) {
        return equipmentService.createType(request);
    }

    @PutMapping("/api/equipment/types/{id}")
    public EquipmentTypeResponse updateType(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentTypeRequest request
    ) {
        return equipmentService.updateType(id, request);
    }

    @DeleteMapping("/api/equipment/types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveType(@PathVariable Long id) {
        equipmentService.archiveType(id);
    }

    @GetMapping("/api/units/{unitId}/equipment")
    public List<UnitEquipmentResponse> byUnit(@PathVariable Long unitId) {
        return equipmentService.byUnit(unitId);
    }

    @PutMapping("/api/units/{unitId}/equipment/{typeId}")
    public UnitEquipmentResponse update(
            @PathVariable Long unitId,
            @PathVariable Long typeId,
            @Valid @RequestBody InventoryQuantityRequest request
    ) {
        return equipmentService.updateUnitEquipment(unitId, typeId, request);
    }

    @DeleteMapping("/api/units/{unitId}/equipment/{typeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long unitId,
            @PathVariable Long typeId
    ) {
        equipmentService.deleteUnitEquipment(unitId, typeId);
    }
}
