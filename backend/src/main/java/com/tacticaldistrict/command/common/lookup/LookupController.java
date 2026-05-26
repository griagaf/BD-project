package com.tacticaldistrict.command.common.lookup;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lookups")
@RequiredArgsConstructor
public class LookupController {

    private final LookupService lookupService;

    @GetMapping("/units")
    public List<LookupOptionResponse> units(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Long formationId
    ) {
        return lookupService.units(search, limit, formationId);
    }

    @GetMapping("/formations")
    public List<LookupOptionResponse> formations(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String types,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Long parentId
    ) {
        return lookupService.formations(search, types, limit, parentId);
    }

    @GetMapping("/subdivisions")
    public List<LookupOptionResponse> subdivisions(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "") String type
    ) {
        return lookupService.subdivisions(search, limit, unitId, parentId, type);
    }

    @GetMapping("/ranks")
    public List<LookupOptionResponse> ranks(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return lookupService.ranks(search, limit);
    }

    @GetMapping("/specialties")
    public List<LookupOptionResponse> specialties(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return lookupService.specialties(search, limit);
    }

    @GetMapping("/equipment-types")
    public List<LookupOptionResponse> equipmentTypes(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return lookupService.equipmentTypes(search, limit);
    }

    @GetMapping("/weapon-types")
    public List<LookupOptionResponse> weaponTypes(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return lookupService.weaponTypes(search, limit);
    }

    @GetMapping("/buildings")
    public List<LookupOptionResponse> buildings(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return lookupService.buildings(search, limit);
    }

    @GetMapping("/personnel")
    public List<LookupOptionResponse> personnel(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long subdivisionId
    ) {
        return lookupService.personnel(search, limit, unitId, subdivisionId);
    }
}
