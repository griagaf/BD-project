package com.tacticaldistrict.command.hierarchy.controller;

import com.tacticaldistrict.command.hierarchy.dto.FocusTreeResponse;
import com.tacticaldistrict.command.hierarchy.dto.HierarchyContextResponse;
import com.tacticaldistrict.command.hierarchy.dto.ObjectPassportResponse;
import com.tacticaldistrict.command.hierarchy.dto.TreeMode;
import com.tacticaldistrict.command.hierarchy.dto.TreeNodeDto;
import com.tacticaldistrict.command.hierarchy.service.HierarchyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hierarchy")
@RequiredArgsConstructor
public class HierarchyController {

    private final HierarchyService hierarchyService;

    @GetMapping("/roots")
    public List<TreeNodeDto> roots(@RequestParam(defaultValue = "STRATEGIC") TreeMode mode) {
        return hierarchyService.roots(mode);
    }

    @GetMapping("/nodes/{nodeType}/{nodeId}/children")
    public List<TreeNodeDto> children(
            @PathVariable String nodeType,
            @PathVariable Long nodeId
    ) {
        return hierarchyService.children(nodeType, nodeId);
    }

    @GetMapping("/focus")
    public FocusTreeResponse focus(
            @RequestParam String nodeType,
            @RequestParam Long nodeId
    ) {
        return hierarchyService.focus(nodeType, nodeId);
    }

    @GetMapping("/personnel/{personnelId}/chain")
    public FocusTreeResponse personnelChain(@PathVariable Long personnelId) {
        return hierarchyService.personnelChain(personnelId);
    }

    @GetMapping("/nodes/{nodeType}/{nodeId}/passport")
    public ObjectPassportResponse passport(
            @PathVariable String nodeType,
            @PathVariable Long nodeId
    ) {
        return hierarchyService.passport(nodeType, nodeId);
    }

    @GetMapping("/nodes/{nodeType}/{nodeId}/context")
    public HierarchyContextResponse context(
            @PathVariable String nodeType,
            @PathVariable Long nodeId
    ) {
        return hierarchyService.context(nodeType, nodeId);
    }
}
