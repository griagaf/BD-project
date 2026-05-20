package com.tacticaldistrict.command.common.attribute;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
public class DynamicAttributeController {

    private final DynamicAttributeService attributeService;

    @GetMapping("/{resource}/types")
    public List<DynamicAttributeMetadataResponse> types(@PathVariable String resource) {
        return attributeService.types(resource);
    }

    @PostMapping("/{resource}/types")
    public DynamicAttributeMetadataResponse createType(
            @PathVariable String resource,
            @Valid @RequestBody DynamicAttributeTypeRequest request
    ) {
        return attributeService.createType(resource, request);
    }

    @GetMapping("/{resource}/categories/{categoryId}")
    public List<DynamicAttributeMetadataResponse> categorySchema(
            @PathVariable String resource,
            @PathVariable Long categoryId
    ) {
        return attributeService.categorySchema(resource, categoryId);
    }

    @PostMapping("/{resource}/categories/{categoryId}/attributes")
    public DynamicAttributeMetadataResponse assignToCategory(
            @PathVariable String resource,
            @PathVariable Long categoryId,
            @Valid @RequestBody AssignAttributeRequest request
    ) {
        return attributeService.assignToCategory(resource, categoryId, request);
    }

    @GetMapping("/ranks/{rankId}")
    public List<DynamicAttributeMetadataResponse> rankSchema(@PathVariable Long rankId) {
        return attributeService.rankSchema(rankId);
    }

    @GetMapping("/ranks/types")
    public List<DynamicAttributeMetadataResponse> rankTypes() {
        return attributeService.rankTypes();
    }

    @PostMapping("/ranks/types")
    public DynamicAttributeMetadataResponse createRankType(@Valid @RequestBody DynamicAttributeTypeRequest request) {
        return attributeService.createRankType(request);
    }

    @PostMapping("/ranks/{rankId}/attributes")
    public DynamicAttributeMetadataResponse assignToRank(
            @PathVariable Long rankId,
            @Valid @RequestBody AssignAttributeRequest request
    ) {
        return attributeService.assignToRank(rankId, request);
    }
}
