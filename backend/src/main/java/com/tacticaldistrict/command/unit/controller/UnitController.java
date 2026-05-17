package com.tacticaldistrict.command.unit.controller;

import com.tacticaldistrict.command.hierarchy.dto.CommanderAssignmentRequest;
import com.tacticaldistrict.command.hierarchy.dto.ObjectPassportResponse;
import com.tacticaldistrict.command.unit.dto.UnitRequest;
import com.tacticaldistrict.command.unit.dto.UnitResponse;
import com.tacticaldistrict.command.unit.service.UnitService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @GetMapping
    public List<UnitResponse> units() {
        return unitService.units();
    }

    @GetMapping("/{id}")
    public UnitResponse unit(@PathVariable Long id) {
        return unitService.unit(id);
    }

    @GetMapping("/{id}/passport")
    public ObjectPassportResponse passport(@PathVariable Long id) {
        return unitService.passport(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnitResponse create(@Valid @RequestBody UnitRequest request) {
        return unitService.create(request);
    }

    @PutMapping("/{id}")
    public UnitResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UnitRequest request
    ) {
        return unitService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        unitService.delete(id);
    }

    @PutMapping("/{id}/commander")
    @PreAuthorize("hasRole('ADMIN_DISTRICT') or @userContextProvider.current().hasPermission('commander:assign')")
    public UnitResponse assignCommander(
            @PathVariable Long id,
            @Valid @RequestBody CommanderAssignmentRequest request
    ) {
        return unitService.assignCommander(id, request);
    }

    @DeleteMapping("/{id}/commander")
    @PreAuthorize("hasRole('ADMIN_DISTRICT') or @userContextProvider.current().hasPermission('commander:assign')")
    public UnitResponse clearCommander(@PathVariable Long id) {
        return unitService.clearCommander(id);
    }
}
