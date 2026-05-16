package com.tacticaldistrict.command.hierarchy.controller;

import com.tacticaldistrict.command.hierarchy.dto.CommanderAssignmentRequest;
import com.tacticaldistrict.command.hierarchy.dto.SubdivisionRequest;
import com.tacticaldistrict.command.hierarchy.dto.SubdivisionResponse;
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
@RequestMapping("/api/subdivisions")
@RequiredArgsConstructor
public class SubdivisionController {

    private final HierarchyService hierarchyService;

    @GetMapping
    public List<SubdivisionResponse> subdivisions() {
        return hierarchyService.subdivisions();
    }

    @GetMapping("/{id}")
    public SubdivisionResponse subdivision(@PathVariable Long id) {
        return hierarchyService.subdivision(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubdivisionResponse create(@Valid @RequestBody SubdivisionRequest request) {
        return hierarchyService.createSubdivision(request);
    }

    @PutMapping("/{id}")
    public SubdivisionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody SubdivisionRequest request
    ) {
        return hierarchyService.updateSubdivision(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hierarchyService.deleteSubdivision(id);
    }

    @PutMapping("/{id}/commander")
    @PreAuthorize("hasRole('ADMIN_DISTRICT') or @userContextProvider.current().hasPermission('commander:assign')")
    public SubdivisionResponse assignCommander(
            @PathVariable Long id,
            @Valid @RequestBody CommanderAssignmentRequest request
    ) {
        return hierarchyService.assignSubdivisionCommander(id, request);
    }

    @DeleteMapping("/{id}/commander")
    @PreAuthorize("hasRole('ADMIN_DISTRICT') or @userContextProvider.current().hasPermission('commander:assign')")
    public SubdivisionResponse clearCommander(@PathVariable Long id) {
        return hierarchyService.clearSubdivisionCommander(id);
    }
}
