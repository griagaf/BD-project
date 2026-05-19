package com.tacticaldistrict.command.weapon.controller;

import com.tacticaldistrict.command.common.dto.PageResponse;
import com.tacticaldistrict.command.equipment.dto.InventoryQuantityRequest;
import com.tacticaldistrict.command.equipment.dto.InventoryStatisticsResponse;
import com.tacticaldistrict.command.equipment.dto.InventoryCategoryRequest;
import com.tacticaldistrict.command.weapon.dto.UnitWeaponResponse;
import com.tacticaldistrict.command.weapon.dto.WeaponCategoryResponse;
import com.tacticaldistrict.command.weapon.dto.WeaponFilter;
import com.tacticaldistrict.command.weapon.dto.WeaponTypePassportResponse;
import com.tacticaldistrict.command.weapon.dto.WeaponTypeRequest;
import com.tacticaldistrict.command.weapon.dto.WeaponTypeResponse;
import com.tacticaldistrict.command.weapon.service.WeaponService;
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
public class WeaponController {

    private final WeaponService weaponService;

    @GetMapping("/api/weapons")
    public PageResponse<UnitWeaponResponse> search(
            @ParameterObject WeaponFilter filter,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return weaponService.search(filter, pageable);
    }

    @GetMapping("/api/weapons/statistics")
    public InventoryStatisticsResponse statistics() {
        return weaponService.statistics();
    }

    @GetMapping("/api/weapons/categories")
    public List<WeaponCategoryResponse> categories() {
        return weaponService.categories();
    }

    @GetMapping("/api/weapons/types")
    public List<WeaponTypeResponse> types() {
        return weaponService.types();
    }

    @GetMapping("/api/weapons/types/{id}/passport")
    public WeaponTypePassportResponse typePassport(@PathVariable Long id) {
        return weaponService.typePassport(id);
    }

    @PostMapping("/api/weapons/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public WeaponCategoryResponse createCategory(@Valid @RequestBody InventoryCategoryRequest request) {
        return weaponService.createCategory(request);
    }

    @PutMapping("/api/weapons/categories/{id}")
    public WeaponCategoryResponse updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody InventoryCategoryRequest request
    ) {
        return weaponService.updateCategory(id, request);
    }

    @DeleteMapping("/api/weapons/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveCategory(@PathVariable Long id) {
        weaponService.archiveCategory(id);
    }

    @PostMapping("/api/weapons/types")
    @ResponseStatus(HttpStatus.CREATED)
    public WeaponTypeResponse createType(@Valid @RequestBody WeaponTypeRequest request) {
        return weaponService.createType(request);
    }

    @PutMapping("/api/weapons/types/{id}")
    public WeaponTypeResponse updateType(
            @PathVariable Long id,
            @Valid @RequestBody WeaponTypeRequest request
    ) {
        return weaponService.updateType(id, request);
    }

    @DeleteMapping("/api/weapons/types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveType(@PathVariable Long id) {
        weaponService.archiveType(id);
    }

    @GetMapping("/api/units/{unitId}/weapons")
    public List<UnitWeaponResponse> byUnit(@PathVariable Long unitId) {
        return weaponService.byUnit(unitId);
    }

    @PutMapping("/api/units/{unitId}/weapons/{typeId}")
    public UnitWeaponResponse update(
            @PathVariable Long unitId,
            @PathVariable Long typeId,
            @Valid @RequestBody InventoryQuantityRequest request
    ) {
        return weaponService.updateUnitWeapon(unitId, typeId, request);
    }

    @DeleteMapping("/api/units/{unitId}/weapons/{typeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long unitId,
            @PathVariable Long typeId
    ) {
        weaponService.deleteUnitWeapon(unitId, typeId);
    }
}
