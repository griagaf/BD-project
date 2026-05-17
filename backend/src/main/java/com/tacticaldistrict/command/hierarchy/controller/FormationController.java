package com.tacticaldistrict.command.hierarchy.controller;

import com.tacticaldistrict.command.hierarchy.dto.CommanderAssignmentRequest;
import com.tacticaldistrict.command.hierarchy.dto.FormationRequest;
import com.tacticaldistrict.command.hierarchy.dto.FormationResponse;
import com.tacticaldistrict.command.hierarchy.service.HierarchyService;
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
@RequestMapping("/api/formations")
@RequiredArgsConstructor
public class FormationController {

    private final HierarchyService hierarchyService;

    @GetMapping
    public List<FormationResponse> formations() {
        return hierarchyService.formations();
    }

    @GetMapping("/{id}")
    public FormationResponse formation(@PathVariable Long id) {
        return hierarchyService.formation(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FormationResponse create(@Valid @RequestBody FormationRequest request) {
        return hierarchyService.createFormation(request);
    }

    @PutMapping("/{id}")
    public FormationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody FormationRequest request
    ) {
        return hierarchyService.updateFormation(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hierarchyService.deleteFormation(id);
    }

    @PutMapping("/{id}/commander")
    @PreAuthorize("hasRole('ADMIN_DISTRICT') or @userContextProvider.current().hasPermission('commander:assign')")
    public FormationResponse assignCommander(
            @PathVariable Long id,
            @Valid @RequestBody CommanderAssignmentRequest request
    ) {
        return hierarchyService.assignFormationCommander(id, request);
    }

    @DeleteMapping("/{id}/commander")
    @PreAuthorize("hasRole('ADMIN_DISTRICT') or @userContextProvider.current().hasPermission('commander:assign')")
    public FormationResponse clearCommander(@PathVariable Long id) {
        return hierarchyService.clearFormationCommander(id);
    }
}
