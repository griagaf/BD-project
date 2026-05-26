package com.tacticaldistrict.command.building.service;

import com.tacticaldistrict.command.audit.AuditService;
import com.tacticaldistrict.command.building.domain.BuildingAssignmentPolicy;
import com.tacticaldistrict.command.building.dto.BuildingFilter;
import com.tacticaldistrict.command.building.dto.BuildingRequest;
import com.tacticaldistrict.command.building.dto.BuildingResponse;
import com.tacticaldistrict.command.building.dto.BuildingStatisticsResponse;
import com.tacticaldistrict.command.building.repository.BuildingJdbcRepository;
import com.tacticaldistrict.command.common.dto.PageResponse;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuildingService {

    private final BuildingJdbcRepository buildingRepository;
    private final BuildingAssignmentPolicy buildingAssignmentPolicy;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<BuildingResponse> search(BuildingFilter filter, Pageable pageable) {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("building:read")) {
            throw new AccessDeniedException("Access denied");
        }
        List<BuildingResponse> rows = buildingRepository.search(filter)
                .stream()
                .filter(row -> permissionService.canRead(user, ObjectType.BUILDING, row.id()))
                .toList();
        return page(rows, pageable);
    }

    @Transactional(readOnly = true)
    public BuildingResponse findById(Long id) {
        UserContext user = userContextProvider.current();
        permissionService.checkRead(user, ObjectType.BUILDING, id);
        return buildingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Building not found: " + id));
    }

    @Transactional
    public BuildingResponse create(BuildingRequest request) {
        UserContext user = userContextProvider.current();
        permissionService.checkCreate(user, ObjectType.MILITARY_UNIT, request.unitId(), ObjectType.BUILDING);
        Long id = buildingRepository.create(request, assignable(request));
        auditService.created(user, ObjectType.BUILDING, id, "Building created");
        return findById(id);
    }

    @Transactional
    public BuildingResponse update(Long id, BuildingRequest request) {
        UserContext user = userContextProvider.current();
        permissionService.checkUpdate(user, ObjectType.BUILDING, id);
        if (!permissionService.canCreate(user, ObjectType.MILITARY_UNIT, request.unitId(), ObjectType.BUILDING)) {
            throw new AccessDeniedException("Access denied");
        }
        if (!buildingRepository.update(id, request, assignable(request))) {
            throw new EntityNotFoundException("Building not found: " + id);
        }
        auditService.updated(user, ObjectType.BUILDING, id, "Building updated");
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        UserContext user = userContextProvider.current();
        permissionService.checkDelete(user, ObjectType.BUILDING, id);
        if (!buildingRepository.delete(id)) {
            throw new EntityNotFoundException("Building not found: " + id);
        }
        auditService.deleted(user, ObjectType.BUILDING, id, "Building deleted");
    }

    @Transactional(readOnly = true)
    public List<BuildingResponse> byUnit(Long unitId) {
        UserContext user = userContextProvider.current();
        permissionService.checkRead(user, ObjectType.MILITARY_UNIT, unitId);
        return buildingRepository.search(new BuildingFilter(null, unitId));
    }

    @Transactional(readOnly = true)
    public BuildingStatisticsResponse statistics() {
        UserContext user = userContextProvider.current();
        List<BuildingResponse> rows = buildingRepository.search(new BuildingFilter(null, null))
                .stream()
                .filter(row -> permissionService.canRead(user, ObjectType.BUILDING, row.id()))
                .toList();
        long units = rows.stream().map(BuildingResponse::unitId).distinct().count();
        long empty = rows.stream().filter(row -> Boolean.TRUE.equals(row.assignable()) && row.subdivisionsCount() == 0).count();
        long overloaded = rows.stream().filter(row -> Boolean.TRUE.equals(row.assignable()) && row.subdivisionsCount() > 3).count();
        long assigned = rows.stream().filter(row -> Boolean.TRUE.equals(row.assignable()) && row.subdivisionsCount() > 0).count();
        int readiness = rows.isEmpty() ? 0 : Math.max(0, Math.min(100, (int) (90 - empty * 8 - overloaded * 10)));
        return new BuildingStatisticsResponse(units, (long) rows.size(), assigned, empty, overloaded, readiness);
    }

    @Transactional
    public void assignSubdivision(Long buildingId, Long subdivisionId) {
        UserContext user = userContextProvider.current();
        permissionService.checkUpdate(user, ObjectType.BUILDING, buildingId);
        BuildingResponse building = findById(buildingId);
        Long subdivisionUnitId = buildingRepository.subdivisionUnitId(subdivisionId)
                .orElseThrow(() -> new EntityNotFoundException("Subdivision not found: " + subdivisionId));
        Long currentBuildingId = buildingRepository.currentBuildingId(subdivisionId).orElse(null);
        buildingAssignmentPolicy.validate(building, subdivisionUnitId, currentBuildingId);
        buildingRepository.assignSubdivision(buildingId, subdivisionId);
        auditService.updated(user, ObjectType.BUILDING, buildingId, "Subdivision assigned to building: " + subdivisionId);
    }

    @Transactional
    public void removeSubdivision(Long buildingId, Long subdivisionId) {
        UserContext user = userContextProvider.current();
        permissionService.checkUpdate(user, ObjectType.BUILDING, buildingId);
        buildingRepository.removeSubdivision(buildingId, subdivisionId);
        auditService.updated(user, ObjectType.BUILDING, buildingId, "Subdivision removed from building: " + subdivisionId);
    }

    private boolean assignable(BuildingRequest request) {
        return request.assignable() == null || request.assignable();
    }

    private PageResponse<BuildingResponse> page(List<BuildingResponse> rows, Pageable pageable) {
        int from = Math.min((int) pageable.getOffset(), rows.size());
        int to = Math.min(from + pageable.getPageSize(), rows.size());
        List<BuildingResponse> content = rows.subList(from, to);
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) rows.size() / pageable.getPageSize());
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), rows.size(), totalPages, pageable.getPageNumber() == 0, pageable.getPageNumber() + 1 >= totalPages);
    }
}
